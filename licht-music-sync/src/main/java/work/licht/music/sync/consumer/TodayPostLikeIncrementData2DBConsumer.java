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
import work.licht.music.sync.model.dto.LikeUnlikePostMqDTO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;


// 日增量数据落库：帖子点赞、取消点赞
@Component
@RocketMQMessageListener(
        consumerGroup = "licht_music_sync_group", // Group 组
        topic = RocketMQConstants.TOPIC_COUNT_POST_LIKE // 主题 Topic
)
@Slf4j
public class TodayPostLikeIncrementData2DBConsumer implements RocketMQListener<String> {

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
        log.info("## TodayPostLikeIncrementData2DBConsumer 消费到了 MQ: {}", body);
        // 消息体 JSON 字符串转 DTO
        LikeUnlikePostMqDTO unlikePostMqDTO = gson.fromJson(body, LikeUnlikePostMqDTO.class);
        if (Objects.isNull(unlikePostMqDTO)) return;
        // 被点赞、取消点赞的帖子 ID
        Long noteId = unlikePostMqDTO.getPostId();
        // 帖子的发布者 ID
        Long noteCreatorId = unlikePostMqDTO.getPostCreatorId();
        // 今日日期
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // ------------------------- 帖子的点赞数变更记录 -------------------------
        // 帖子对应的 Bloom Key
        String postBloomKey = RedisKeyConstants.buildBloomPostLikePostIdListKey(date);
        // 1. 布隆过滤器判断该日增量数据是否已经记录
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_today_note_like_check.lua")));
        // 返回值类型
        script.setResultType(Long.class);
        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(postBloomKey), noteId);
        // Lua 脚本：添加到布隆过滤器
        RedisScript<Long> bloomAddScript = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);
        // 若布隆过滤器判断不存在（绝对正确）
        if (Objects.equals(result, 0L)) {
            // 2. 若无，才会落库，减轻数据库压力
            // 根据分片总数，取模，获取对应的分片序号
            long postIdHashKey = noteId % tableShards;
            try {
                // 将日增量变更数据落库
                // t_data_align_note_like_count_temp_日期_分片序号
                insertMapper.insert2SyncPostLikeCountTempTable(TableConstants.buildTableNameSuffix(date, postIdHashKey), noteId);
            } catch (Exception e) {
                log.error("TodayPostLikeIncrementData2DBConsumer：日增量变更数据写入失败", e);
            }
            // 4. 数据库写入成功后，再添加布隆过滤器中
            redisTemplate.execute(bloomAddScript, Collections.singletonList(postBloomKey), noteId);
        }
        // ------------------------- 帖子发布者获得的点赞数变更记录 -------------------------
        // 帖子发布者对应的 Bloom Key
        String userBloomKey = RedisKeyConstants.buildBloomPostLikeUserIdListKey(date);
        // 执行 Lua 脚本，拿到返回结果
        result = redisTemplate.execute(script, Collections.singletonList(userBloomKey), noteCreatorId);
        // 若布隆过滤器判断不存在（绝对正确）
        if (Objects.equals(result, 0L)) {
            // 2. 若无，才会落库，减轻数据库压力
            // 根据分片总数，取模，获取对应的分片序号
            long userIdHashKey = noteCreatorId % tableShards;
            try {
                // 将日增量变更数据落库
                // t_data_align_user_like_count_temp_日期_分片序号
                insertMapper.insert2SyncUserLikeCountTempTable(TableConstants.buildTableNameSuffix(date, userIdHashKey), noteCreatorId);
            } catch (Exception e) {
                log.error("TodayPostLikeIncrementData2DBConsumer：日增量变更数据写入失败", e);
            }
            // 4. 数据库写入成功后，再添加布隆过滤器中
            redisTemplate.execute(bloomAddScript, Collections.singletonList(userBloomKey), noteCreatorId);
        }
    }

}