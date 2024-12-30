package work.licht.music.relation.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import work.licht.music.common.util.DateUtils;
import work.licht.music.relation.constant.RedisKeyConstants;
import work.licht.music.relation.constant.RocketMQConstants;
import work.licht.music.relation.domain.mapper.FanDOMapper;
import work.licht.music.relation.domain.mapper.FollowerDOMapper;
import work.licht.music.relation.domain.model.FanDO;
import work.licht.music.relation.domain.model.FollowerDO;
import work.licht.music.relation.model.vo.FollowUserMqDTO;
import work.licht.music.relation.model.vo.UnfollowUserMqDTO;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;

@Slf4j
@Component
@RocketMQMessageListener(
    consumerGroup = "licht_music_group", // Group 组
    topic = RocketMQConstants.TOPIC_FOLLOW_OR_UNFOLLOW, // 消费的 Topic 主题
    consumeMode = ConsumeMode.ORDERLY // 顺序消费
)
public class FollowUnfollowConsumer implements RocketMQListener<Message> {

    @Resource
    private FollowerDOMapper followerDOMapper;
    @Resource
    private FanDOMapper fanDOMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private TransactionTemplate transactionTemplate;
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
                int count = followerDOMapper.insert(FollowerDO.builder()
                        .userId(userId)
                        .followerUserId(followUserId)
                        .createTime(createTime)
                        .build());
                // 粉丝表：一条记录
                if (count > 0) {
                    FanDO fanDO = FanDO.builder()
                            .userId(followUserId)
                            .fanUserId(userId)
                            .createTime(createTime)
                            .build();
                    fanDOMapper.insert(fanDO);
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
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_update_fan_zset.lua")));
            script.setResultType(Long.class);
            // 时间戳
            long timestamp = DateUtils.localDateTime2Timestamp(createTime);
            // 构建被关注用户的粉丝列表 Redis Key
            String fanRedisKey = RedisKeyConstants.buildUserFanKey(followUserId);
            // 执行脚本
            redisTemplate.execute(script, Collections.singletonList(fanRedisKey), userId, timestamp);
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
                int count = followerDOMapper.deleteByUserIdAndFollowerUserId(userId, unfollowUserId);
                // 粉丝表：一条记录
                if (count > 0) fanDOMapper.deleteByUserIdAndFanUserId(unfollowUserId, userId);
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
            String fanRedisKey = RedisKeyConstants.buildUserFanKey(unfollowUserId);
            // 删除指定粉丝
            redisTemplate.opsForZSet().remove(fanRedisKey, userId);
        }
    }


}