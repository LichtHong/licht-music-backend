package work.licht.music.post.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import work.licht.music.post.constant.RocketMQConstants;
import work.licht.music.post.domain.mapper.PostCollectDOMapper;
import work.licht.music.post.domain.model.PostCollectDO;
import work.licht.music.post.model.dto.CollectUnCollectPostMqDTO;

import java.time.LocalDateTime;
import java.util.Objects;

// 帖子收藏、取消收藏 MQ 消费者
@Component
@RocketMQMessageListener(
        consumerGroup = "licht_music_post_group", // Group 组
        topic = RocketMQConstants.TOPIC_COLLECT_OR_UN_COLLECT, // 消费的主题 Topic
        consumeMode = ConsumeMode.ORDERLY // 设置为顺序消费模式
)
@Slf4j
public class CollectUnCollectPostConsumer implements RocketMQListener<Message> {

    @Resource
    private PostCollectDOMapper postCollectDOMapper;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private Gson gson;

    // 每秒创建 5000 个令牌
    private final RateLimiter rateLimiter = RateLimiter.create(5000);

    @Override
    public void onMessage(Message message) {
        // 流量削峰：通过获取令牌，如果没有令牌可用，将阻塞，直到获得
        rateLimiter.acquire();
        // 幂等性: 通过联合唯一索引保证
        // 消息体
        String bodyJsonStr = new String(message.getBody());
        // 标签
        String tags = message.getTags();

        log.info("==> CollectUnCollectPostConsumer 消费了消息 {}, tags: {}", bodyJsonStr, tags);

        // 根据 MQ 标签，判断操作类型
        if (Objects.equals(tags, RocketMQConstants.TAG_COLLECT)) {
            // 收藏帖子
            handleCollectPostTagMessage(bodyJsonStr);
        } else if (Objects.equals(tags, RocketMQConstants.TAG_UN_COLLECT)) {
            // 取消收藏帖子
            handleUnCollectPostTagMessage(bodyJsonStr);
        }
    }

    // 帖子收藏
    private void handleCollectPostTagMessage(String bodyJsonStr) {
        // 消息体 JSON 字符串转 DTO
        CollectUnCollectPostMqDTO collectUnCollectPostMqDTO = gson.fromJson(bodyJsonStr, CollectUnCollectPostMqDTO.class);
        if (Objects.isNull(collectUnCollectPostMqDTO)) return;
        // 用户ID
        Long userId = collectUnCollectPostMqDTO.getUserId();
        // 收藏的帖子ID
        Long postId = collectUnCollectPostMqDTO.getPostId();
        // 操作类型
        Integer type = collectUnCollectPostMqDTO.getType();
        // 收藏时间
        LocalDateTime createTime = collectUnCollectPostMqDTO.getCreateTime();
        // 构建 DO 对象
        PostCollectDO postCollectDO = PostCollectDO.builder()
                .userId(userId)
                .postId(postId)
                .createTime(createTime)
                .status(type)
                .build();
        // 添加或更新帖子收藏记录
        int count = postCollectDOMapper.insertOrUpdate(postCollectDO);
        if (count == 0) return;
        // 更新数据库成功后，发送计数 MQ
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyJsonStr).build();
        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(RocketMQConstants.TOPIC_COUNT_POST_COLLECT, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 帖子收藏】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 帖子收藏】MQ 发送异常: ", throwable);
            }
        });
    }

    // 帖子取消收藏
    private void handleUnCollectPostTagMessage(String bodyJsonStr) {
        // 消息体 JSON 字符串转 DTO
        CollectUnCollectPostMqDTO unCollectPostMqDTO = gson.fromJson(bodyJsonStr, CollectUnCollectPostMqDTO.class);
        if (Objects.isNull(unCollectPostMqDTO)) return;
        // 用户ID
        Long userId = unCollectPostMqDTO.getUserId();
        // 收藏的帖子ID
        Long postId = unCollectPostMqDTO.getPostId();
        // 操作类型
        Integer type = unCollectPostMqDTO.getType();
        // 收藏时间
        LocalDateTime createTime = unCollectPostMqDTO.getCreateTime();
        // 构建 DO 对象
        PostCollectDO postCollectDO = PostCollectDO.builder()
                .userId(userId)
                .postId(postId)
                .createTime(createTime)
                .status(type)
                .build();
        // 取消收藏：记录更新
        int count = postCollectDOMapper.update2UnCollectByUserIdAndPostId(postCollectDO);
        if (count == 0) return;
        // 更新数据库成功后，发送计数 MQ
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyJsonStr).build();
        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(RocketMQConstants.TOPIC_COUNT_POST_COLLECT, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 帖子取消收藏】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 帖子取消收藏】MQ 发送异常: ", throwable);
            }
        });
    }

}