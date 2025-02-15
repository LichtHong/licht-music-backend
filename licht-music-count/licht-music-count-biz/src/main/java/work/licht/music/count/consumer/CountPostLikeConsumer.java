package work.licht.music.count.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import work.licht.music.count.constant.RedisKeyConstants;
import work.licht.music.count.constant.RocketMQConstants;
import work.licht.music.count.enums.LikeUnlikePostTypeEnum;
import work.licht.music.count.model.dto.AggregationCountLikeUnlikePostMqDTO;
import work.licht.music.count.model.dto.CountLikeUnlikePostMqDTO;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// 计数: 帖子点赞数
@Component
@RocketMQMessageListener(
        consumerGroup = "licht_music_count_group", // Group 组
        topic = RocketMQConstants.TOPIC_COUNT_POST_LIKE // 主题 Topic
)
@Slf4j
public class CountPostLikeConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private Gson gson;

    private final BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking().bufferSize(50000) // 缓存队列的最大容量
            .batchSize(1000)   // 一次最多聚合 1000 条
            .linger(Duration.ofSeconds(1)) // 多久聚合一次
            .setConsumerEx(this::consumeMessage) // 设置消费者方法
            .build();

    @Override
    public void onMessage(String body) {
        // 往 bufferTrigger 中添加元素
        bufferTrigger.enqueue(body);
    }

    private void consumeMessage(List<String> messages) {
        log.info("==> 【帖子点赞数】聚合消息, size: {}", messages.size());
        log.info("==> 【帖子点赞数】聚合消息, {}", gson.toJson(messages));
        // List<String> 转 List<CountLikeUnlikePostMqDTO>
        List<CountLikeUnlikePostMqDTO> countLikeUnlikePostMqDTOList = messages.stream().map(body -> gson.fromJson(body, CountLikeUnlikePostMqDTO.class)).toList();
        // 按帖子 ID 进行分组
        Map<Long, List<CountLikeUnlikePostMqDTO>> groupMap = countLikeUnlikePostMqDTOList.stream().collect(Collectors.groupingBy(CountLikeUnlikePostMqDTO::getPostId));
        // 按组汇总数据，统计出最终的计数
        // 最终操作的计数对象
        List<AggregationCountLikeUnlikePostMqDTO> countList = Lists.newArrayList();
        for (Map.Entry<Long, List<CountLikeUnlikePostMqDTO>> entry : groupMap.entrySet()) {
            // 帖子 ID
            Long postId = entry.getKey();
            // 帖子发布者 ID
            Long creatorId = null;
            List<CountLikeUnlikePostMqDTO> list = entry.getValue();
            // 最终计数值，默认为 0
            int finalCount = 0;
            for (CountLikeUnlikePostMqDTO countLikeUnlikePostMqDTO : list) {
                // 设置帖子发布者用户 ID
                creatorId = countLikeUnlikePostMqDTO.getPostCreatorId();
                // 获取操作类型
                Integer type = countLikeUnlikePostMqDTO.getType();
                // 根据操作类型，获取对应枚举
                LikeUnlikePostTypeEnum likeUnlikeNoteTypeEnum = LikeUnlikePostTypeEnum.valueOf(type);
                // 若枚举为空，跳到下一次循环
                if (Objects.isNull(likeUnlikeNoteTypeEnum)) continue;
                switch (likeUnlikeNoteTypeEnum) {
                    case LIKE -> finalCount += 1; // 如果为点赞操作，点赞数 +1
                    case UNLIKE -> finalCount -= 1; // 如果为取消点赞操作，点赞数 -1
                }
            }
            // 将分组后统计出的最终计数，存入 countList 中
            countList.add(AggregationCountLikeUnlikePostMqDTO.builder()
                    .postId(postId)
                    .creatorId(creatorId)
                    .count(finalCount)
                    .build());
        }
        log.info("## 【帖子点赞数】聚合后的计数数据: {}", gson.toJson(countList));
        // 更新 Redis
        countList.forEach( item -> {
            // 帖子发布者 ID
            Long creatorId = item.getCreatorId();
            // 帖子 ID
            Long postId = item.getPostId();
            // 聚合后的计数
            Integer count = item.getCount();
            // Redis Key
            String redisKey = RedisKeyConstants.buildCountPostKey(postId);
            // 判断 Redis 中 Hash 是否存在
            boolean isExisted = redisTemplate.hasKey(redisKey);
            // 存在才会去更新，初始化工作放在查询计数来做
            if (isExisted) {
                // 对目标用户 Hash 中的点赞数字段进行计数操作
                redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_LIKE_TOTAL, count);
            }
            // 更新 Redis 用户维度点赞数
            String countUserRedisKey = RedisKeyConstants.buildCountUserKey(creatorId);
            boolean isCountUserExisted = redisTemplate.hasKey(countUserRedisKey);
            if (isCountUserExisted) {
                redisTemplate.opsForHash().increment(countUserRedisKey, RedisKeyConstants.FIELD_LIKE_TOTAL, count);
            }
        });
        // 发送 MQ 帖子点赞数据落库
        Message<String> message = MessageBuilder.withPayload(gson.toJson(countList)).build();
        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(RocketMQConstants.TOPIC_COUNT_POST_LIKE_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：帖子点赞数入库】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：帖子点赞数入库】MQ 发送异常: ", throwable);
            }
        });
    }
}
