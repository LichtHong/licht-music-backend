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
import work.licht.music.count.constant.RocketMQConstants;
import work.licht.music.count.domain.mapper.UserCountDOMapper;

import java.util.Map;

@Component
@RocketMQMessageListener(
    consumerGroup = "licht_music_count_group", // Group 组
    topic = RocketMQConstants.TOPIC_COUNT_FOLLOWER_2_DB // 主题 Topic
)
@Slf4j
public class CountFollower2DBConsumer implements RocketMQListener<String> {

    @Resource
    private UserCountDOMapper userCountDOMapper;
    // 每秒创建 5000 个令牌
    private final RateLimiter rateLimiter = RateLimiter.create(5000);
    @Resource
    private Gson gson;

    @Override
    public void onMessage(String body) {
        // 流量削峰：通过获取令牌，如果没有令牌可用，将阻塞，直到获得
        rateLimiter.acquire();
        log.info("## 消费到了 MQ 【计数: 粉丝数入库】, {}...", body);
        Map<Long, Integer> countMap = null;
        try {
            countMap = gson.fromJson(body, new TypeToken<>() {}.getType());
        } catch (Exception e) {
            log.error("## 解析 JSON 字符串异常", e);
        }
        if (CollUtil.isNotEmpty(countMap)) {
            // 判断数据库中，若目标用户的记录不存在，则插入；若记录已存在，则直接更新
            countMap.forEach((k, v) -> userCountDOMapper.insertOrUpdateFollowerTotalByUserId(v, k));
        }
    }

}

