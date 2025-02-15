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
import work.licht.music.post.domain.mapper.PostLikeDOMapper;
import work.licht.music.post.domain.model.PostLikeDO;
import work.licht.music.post.model.dto.LikeUnlikePostMqDTO;

import java.time.LocalDateTime;
import java.util.Objects;

// 帖子点赞、取消点赞 MQ 消费者
@Component
@RocketMQMessageListener(
        consumerGroup = "licht_music_post_group", // Group 组
        topic = RocketMQConstants.TOPIC_LIKE_OR_UNLIKE, // 消费的主题 Topic
        consumeMode = ConsumeMode.ORDERLY // 设置为顺序消费模式
)
@Slf4j
public class LikeUnlikePostConsumer implements RocketMQListener<Message> {

    @Resource
    private PostLikeDOMapper postLikeDOMapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    // 每秒创建 5000 个令牌
    private final RateLimiter rateLimiter = RateLimiter.create(5000);
    @Resource
    private Gson gson;

    @Override
    public void onMessage(Message message) {
        // 流量削峰：通过获取令牌，如果没有令牌可用，将阻塞，直到获得
        rateLimiter.acquire();
        // 幂等性: 通过联合唯一索引保证
        // 消息体
        String bodyJsonStr = new String(message.getBody());
        // 标签
        String tags = message.getTags();
        log.info("==> LikeUnlikePostConsumer 消费了消息 {}, tags: {}", bodyJsonStr, tags);
        // 根据 MQ 标签，判断操作类型
        if (Objects.equals(tags, RocketMQConstants.TAG_LIKE)) {
            // 点赞帖子
            handleLikePostTagMessage(bodyJsonStr);
        } else if (Objects.equals(tags, RocketMQConstants.TAG_UNLIKE)) {
            // 取消点赞帖子
            handleUnlikePostTagMessage(bodyJsonStr);
        }
    }

    // 帖子点赞
    private void handleLikePostTagMessage(String bodyJsonStr) {
        // 消息体 JSON 字符串转 DTO
        LikeUnlikePostMqDTO likePostMqDTO = gson.fromJson(bodyJsonStr, LikeUnlikePostMqDTO.class);
        if (Objects.isNull(likePostMqDTO)) return;
        // 用户ID
        Long userId = likePostMqDTO.getUserId();
        // 点赞的帖子ID
        Long postId = likePostMqDTO.getPostId();
        // 操作类型
        Integer type = likePostMqDTO.getType();
        // 点赞时间
        LocalDateTime createTime = likePostMqDTO.getCreateTime();
        // 构建 DO 对象
        PostLikeDO postLikeDO = PostLikeDO.builder()
                .userId(userId)
                .postId(postId)
                .createTime(createTime)
                .status(type)
                .build();
        // 添加或更新帖子点赞记录
        int count = postLikeDOMapper.insertOrUpdate(postLikeDO);
        if (count == 0) return;
        // 更新数据库成功后，发送计数 MQ
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyJsonStr).build();
        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(RocketMQConstants.TOPIC_COUNT_POST_LIKE, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 帖子点赞】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 帖子点赞】MQ 发送异常: ", throwable);
            }
        });
    }

    // 帖子取消点赞
    private void handleUnlikePostTagMessage(String bodyJsonStr) {
        // 消息体 JSON 字符串转 DTO
        LikeUnlikePostMqDTO likeUnlikePostMqDTO = gson.fromJson(bodyJsonStr, LikeUnlikePostMqDTO.class);
        if (Objects.isNull(likeUnlikePostMqDTO)) return;
        // 用户ID
        Long userId = likeUnlikePostMqDTO.getUserId();
        // 点赞的帖子ID
        Long postId = likeUnlikePostMqDTO.getPostId();
        // 操作类型
        Integer type = likeUnlikePostMqDTO.getType();
        // 点赞时间
        LocalDateTime createTime = likeUnlikePostMqDTO.getCreateTime();
        // 构建 DO 对象
        PostLikeDO postLikeDO = PostLikeDO.builder()
                .userId(userId)
                .postId(postId)
                .createTime(createTime)
                .status(type)
                .build();
        // 取消点赞：记录更新
        int count = postLikeDOMapper.update2UnlikeByUserIdAndPostId(postLikeDO);
        if (count == 0) return;
        // 更新数据库成功后，发送计数 MQ
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyJsonStr).build();
        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(RocketMQConstants.TOPIC_COUNT_POST_LIKE, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数: 帖子取消点赞】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数: 帖子取消点赞】MQ 发送异常: ", throwable);
            }
        });
    }

}

