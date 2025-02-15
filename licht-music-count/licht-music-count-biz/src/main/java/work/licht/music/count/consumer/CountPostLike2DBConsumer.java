package work.licht.music.count.consumer;

import cn.hutool.core.collection.CollUtil;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import work.licht.music.count.constant.RocketMQConstants;
import work.licht.music.count.domain.mapper.PostCountDOMapper;
import work.licht.music.count.domain.mapper.UserCountDOMapper;
import work.licht.music.count.model.dto.AggregationCountLikeUnlikePostMqDTO;

import java.util.List;

// 计数: 帖子点赞数落库
@Component
@RocketMQMessageListener(
        consumerGroup = "licht_music_count_group_", // Group 组
        topic = RocketMQConstants.TOPIC_COUNT_POST_LIKE_2_DB // 主题 Topic
)
@Slf4j
public class CountPostLike2DBConsumer implements RocketMQListener<String> {

    @Resource
    private PostCountDOMapper postCountDOMapper;
    @Resource
    private UserCountDOMapper userCountDOMapper;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private Gson gson;

    // 每秒创建 5000 个令牌
    private final RateLimiter rateLimiter = RateLimiter.create(5000);

    @Override
    public void onMessage(String body) {
        // 流量削峰：通过获取令牌，如果没有令牌可用，将阻塞，直到获得
        rateLimiter.acquire();
        log.info("## 消费到了 MQ 【计数:  帖子点赞数入库】, {}...", body);
        List<AggregationCountLikeUnlikePostMqDTO> countList = null;
        try {
            countList = gson.fromJson(body, new TypeToken<>(){});
        } catch (Exception e) {
            log.error("## 解析 JSON 字符串异常", e);
        }
        if (CollUtil.isNotEmpty(countList)) {
            // 判断数据库中 t_user_count 和 t_post_count 表，若帖子计数记录不存在，则插入；若记录已存在，则直接更新
            countList.forEach(item -> {
                Long creatorId = item.getCreatorId();
                Long postId = item.getPostId();
                Integer count = item.getCount();
                // 编程式事务，保证两条语句的原子性
                transactionTemplate.execute(status -> {
                    try {
                        postCountDOMapper.insertOrUpdateLikeTotalByPostId(count, postId);
                        userCountDOMapper.insertOrUpdateLikeTotalByUserId(count, creatorId);
                        return true;
                    } catch (Exception ex) {
                        status.setRollbackOnly();
                        // 标记事务为回滚
                        log.error("", ex);
                    }
                    return false;
                });
            });
        }
    }

}