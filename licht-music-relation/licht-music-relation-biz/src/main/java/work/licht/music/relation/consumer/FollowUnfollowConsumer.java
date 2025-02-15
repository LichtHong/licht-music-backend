package work.licht.music.relation.consumer;

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
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import work.licht.music.common.util.DateUtils;
import work.licht.music.relation.constant.RedisKeyConstants;
import work.licht.music.relation.constant.RocketMQConstants;
import work.licht.music.relation.domain.mapper.FollowerDOMapper;
import work.licht.music.relation.domain.mapper.FollowingDOMapper;
import work.licht.music.relation.domain.model.FollowerDO;
import work.licht.music.relation.domain.model.FollowingDO;
import work.licht.music.relation.enums.FollowUnfollowTypeEnum;
import work.licht.music.relation.model.dto.CountFollowUnfollowMqDTO;
import work.licht.music.relation.model.dto.FollowUserMqDTO;
import work.licht.music.relation.model.dto.UnfollowUserMqDTO;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;

@Slf4j
@Component
@RocketMQMessageListener(
    consumerGroup = "licht_music_relation_group", // Group 组
    topic = RocketMQConstants.TOPIC_FOLLOW_OR_UNFOLLOW, // 消费的 Topic 主题
    consumeMode = ConsumeMode.ORDERLY // 顺序消费
)
public class FollowUnfollowConsumer implements RocketMQListener<Message> {

    @Resource
    private FollowingDOMapper followingDOMapper;
    @Resource
    private FollowerDOMapper followerDOMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private RateLimiter rateLimiter;
    @Resource
    private Gson gson;

    @Override
    public void onMessage(Message message) {
        // 流量削峰：通过获取令牌，如果没有令牌可用，将阻塞，直到获得
        rateLimiter.acquire();
        // 标签
        String tags = message.getTags();
        // 消息体
        String bodyJsonStr = new String(message.getBody());
        log.info("==> FollowUnfollowConsumer 消费了消息 {}, tags: {}", bodyJsonStr, tags);
        // 根据 MQ 标签，判断操作类型
        if (Objects.equals(tags, RocketMQConstants.TAG_FOLLOW)) {
            // 关注
            handleFollowTagMessage(bodyJsonStr);
        } else if (Objects.equals(tags, RocketMQConstants.TAG_UNFOLLOW)) {
            // 取关
            handleUnfollowTagMessage(bodyJsonStr);
        }
    }

    private void handleFollowTagMessage(String bodyJsonStr) {
        // 将消息体 Json 字符串转为 DTO 对象
        FollowUserMqDTO followUserMqDTO = gson.fromJson(bodyJsonStr, FollowUserMqDTO.class);
        // 判空
        if (Objects.isNull(followUserMqDTO)) return;
        // 幂等性：通过联合唯一索引保证
        Long userId = followUserMqDTO.getUserId();
        Long followUserId = followUserMqDTO.getFollowUserId();
        LocalDateTime createTime = followUserMqDTO.getCreateTime();
        // 编程式提交事务
        boolean isSuccess = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                // 关注成功需往数据库添加两条记录
                // 关注表：一条记录
                int count = followingDOMapper.insert(FollowingDO.builder()
                        .userId(userId)
                        .followingUserId(followUserId)
                        .createTime(createTime)
                        .build());
                // 粉丝表：一条记录
                if (count > 0) {
                    FollowerDO followerDO = FollowerDO.builder()
                            .userId(followUserId)
                            .followerUserId(userId)
                            .createTime(createTime)
                            .build();
                    followerDOMapper.insert(followerDO);
                }
                return true;
            } catch (Exception ex) {
                // 标记事务为回滚
                status.setRollbackOnly();
                log.error("数据库添加关注粉丝记录失败", ex);
            }
            return false;
        }));
        // 若数据库操作成功，更新 Redis 中被关注用户的 ZSet 粉丝列表
        if (isSuccess) {
            // Lua 脚本
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_update_follower_zset.lua")));
            script.setResultType(Long.class);
            // 时间戳
            long timestamp = DateUtils.localDateTime2Timestamp(createTime);
            // 构建被关注用户的粉丝列表 Redis Key
            String followerRedisKey = RedisKeyConstants.buildUserFollowerKey(followUserId);
            // 执行脚本
            redisTemplate.execute(script, Collections.singletonList(followerRedisKey), userId, timestamp);
            // 发送 MQ 通知计数服务：统计关注数
            // 构建消息体 DTO
            CountFollowUnfollowMqDTO countFollowUnfollowMqDTO = CountFollowUnfollowMqDTO.builder()
                    .userId(userId)
                    .targetUserId(followUserId)
                    .type(FollowUnfollowTypeEnum.FOLLOW.getCode()) // 关注
                    .build();
            // 发送 MQ
            sendMQ(countFollowUnfollowMqDTO);
        }
    }

    // 取关
    private void handleUnfollowTagMessage(String bodyJsonStr) {
        // 将消息体 Json 字符串转为 DTO 对象
        UnfollowUserMqDTO unfollowUserMqDTO = gson.fromJson(bodyJsonStr, UnfollowUserMqDTO.class);
        // 判空
        if (Objects.isNull(unfollowUserMqDTO)) return;
        Long userId = unfollowUserMqDTO.getUserId();
        Long unfollowUserId = unfollowUserMqDTO.getUnfollowUserId();
        LocalDateTime createTime = unfollowUserMqDTO.getCreateTime();
        // 编程式提交事务
        boolean isSuccess = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                // 取关成功需要删除数据库两条记录
                // 关注表：一条记录
                int count = followingDOMapper.deleteByUserIdAndFollowingUserId(userId, unfollowUserId);
                // 粉丝表：一条记录
                if (count > 0) followerDOMapper.deleteByUserIdAndFollowerUserId(unfollowUserId, userId);
                return true;
            } catch (Exception ex) {
                status.setRollbackOnly(); // 标记事务为回滚
                log.error("", ex);
            }
            return false;
        }));
        // 若数据库删除成功，更新 Redis，将自己从被取注用户的 ZSet 粉丝列表删除
        if (isSuccess) {
            // 被取关用户的粉丝列表 Redis Key
            String followerRedisKey = RedisKeyConstants.buildUserFollowerKey(unfollowUserId);
            // 删除指定粉丝
            redisTemplate.opsForZSet().remove(followerRedisKey, userId);
        }
    }

    // 发送 MQ 通知计数服务
    private void sendMQ(CountFollowUnfollowMqDTO countFollowUnfollowMqDTO) {
        // 构建消息对象，并将 DTO 转成 Json 字符串设置到消息体中
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(gson.toJson(countFollowUnfollowMqDTO))
                .build();
        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(RocketMQConstants.TOPIC_COUNT_FOLLOWING, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：关注数】MQ 发送成功，SendResult: {}", sendResult);
            }
            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：关注数】MQ 发送异常: ", throwable);
            }
        });
        // 发送 MQ 通知计数服务：统计粉丝数
        rocketMQTemplate.asyncSend(RocketMQConstants.TOPIC_COUNT_FOLLOWER, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：粉丝数】MQ 发送成功，SendResult: {}", sendResult);
            }
            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：粉丝数】MQ 发送异常: ", throwable);
            }
        });
    }
}