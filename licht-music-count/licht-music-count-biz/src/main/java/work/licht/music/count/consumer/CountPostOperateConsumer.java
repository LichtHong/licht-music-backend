package work.licht.music.count.consumer;

import com.google.gson.Gson;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import work.licht.music.count.constant.RedisKeyConstants;
import work.licht.music.count.constant.RocketMQConstants;
import work.licht.music.count.domain.mapper.UserCountDOMapper;
import work.licht.music.count.model.dto.PostOperateMqDTO;

import java.util.Objects;

// 计数: 帖子发布数

@Component
@RocketMQMessageListener(
        consumerGroup = "licht_music_count_group", // Group 组
        topic = RocketMQConstants.TOPIC_POST_OPERATE // 主题 Topic
)
@Slf4j
public class CountPostOperateConsumer implements RocketMQListener<Message> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private UserCountDOMapper userCountDOMapper;
    @Resource
    private Gson gson;

    @Override
    public void onMessage(Message message) {
        // 消息体
        String bodyJsonStr = new String(message.getBody());
        // 标签
        String tags = message.getTags();
        log.info("==> CountPostOperateConsumer 消费了消息 {}, tags: {}", bodyJsonStr, tags);
        // 根据 MQ 标签，判断帖子操作类型
        if (Objects.equals(tags, RocketMQConstants.TAG_POST_PUBLISH)) {
            // 帖子发布
            handleTagMessage(bodyJsonStr, 1);
        } else if (Objects.equals(tags, RocketMQConstants.TAG_POST_DELETE)) {
            // 帖子删除
            handleTagMessage(bodyJsonStr, -1);
        }
    }

    // 帖子发布、删除
    private void handleTagMessage(String bodyJsonStr, long count) {
        // 消息体 JSON 字符串转 DTO
        PostOperateMqDTO postOperateMqDTO = gson.fromJson(bodyJsonStr, PostOperateMqDTO.class);
        if (Objects.isNull(postOperateMqDTO)) return;
        // 帖子发布者 ID
        Long creatorId = postOperateMqDTO.getCreatorId();
        // 更新 Redis 中用户维度的计数 Hash
        String countUserRedisKey = RedisKeyConstants.buildCountUserKey(creatorId);
        // 判断 Redis 中 Hash 是否存在
        boolean isCountUserExisted = redisTemplate.hasKey(countUserRedisKey);
        // 若存在才会更新
        // (因为缓存设有过期时间，考虑到过期后，缓存会被删除，这里需要判断一下，存在才会去更新，而初始化工作放在查询计数来做)
        if (isCountUserExisted) {
            // 对目标用户 Hash 中的帖子发布总数，进行加减操作
            redisTemplate.opsForHash().increment(countUserRedisKey, RedisKeyConstants.FIELD_PUBLISH_TOTAL, count);
        }
        // 更新 t_user_count 表
        userCountDOMapper.insertOrUpdatePublishTotalByUserId(count, creatorId);
    }

}

