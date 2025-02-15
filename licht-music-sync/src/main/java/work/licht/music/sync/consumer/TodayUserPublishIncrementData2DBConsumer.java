package work.licht.music.sync.consumer;

import com.google.gson.Gson;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import work.licht.music.sync.constant.RedisKeyConstants;
import work.licht.music.sync.constant.RocketMQConstants;
import work.licht.music.sync.constant.TableConstants;
import work.licht.music.sync.domain.mapper.InsertMapper;
import work.licht.music.sync.model.dto.PostOperateMqDTO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;

// 日增量数据落库：帖子发布、删除
@Component
@RocketMQMessageListener(
        consumerGroup = "licht_music_sync_group", // Group 组
        topic = RocketMQConstants.TOPIC_POST_OPERATE // 主题 Topic
)
@Slf4j
public class TodayUserPublishIncrementData2DBConsumer implements RocketMQListener<String> {

    @Resource
    private InsertMapper insertMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private Gson gson;

    // 表总分片数
    @Value("${table.shards}")
    private int tableShards;

    @Override
    public void onMessage(String body) {
        log.info("## TodayPostPublishIncrementData2DBConsumer 消费到了 MQ: {}", body);
        // 消息体 JSON 字符串转 DTO
        PostOperateMqDTO postOperateMqDTO = gson.fromJson(body, PostOperateMqDTO.class);
        if (Objects.isNull(postOperateMqDTO)) return;
        // 发布、被删除帖子发布者 ID
        Long postCreatorId = postOperateMqDTO.getCreatorId();
        // 今日日期
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String bloomKey = RedisKeyConstants.buildBloomPostOperateListKey(date);
        // 1. 布隆过滤器判断该日增量数据是否已经记录
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_today_post_publish_check.lua")));
        // 返回值类型
        script.setResultType(Long.class);
        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(bloomKey), postCreatorId);
        // 若布隆过滤器判断不存在（绝对正确）
        if (Objects.equals(result, 0L)) {
            // 2. 若无，才会落库，减轻数据库压力
            // 根据分片总数，取模，分别获取对应的分片序号
            long userIdHashKey = postCreatorId % tableShards;
            // 将日增量变更数据，写入日增量表中
            // t_sync_user_publish_count_temp_日期_分片序号
            insertMapper.insert2SyncUserPublishCountTempTable(TableConstants.buildTableNameSuffix(date, userIdHashKey), postCreatorId);
            // 3. 数据库写入成功后，再添加布隆过滤器中
            RedisScript<Long> bloomAddScript = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);
            redisTemplate.execute(bloomAddScript, Collections.singletonList(bloomKey), postCreatorId);
        }
    }

}