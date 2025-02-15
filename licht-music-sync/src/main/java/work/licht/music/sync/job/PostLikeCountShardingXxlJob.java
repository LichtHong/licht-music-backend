package work.licht.music.sync.job;

import cn.hutool.core.collection.CollUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import work.licht.music.sync.constant.RedisKeyConstants;
import work.licht.music.sync.constant.TableConstants;
import work.licht.music.sync.domain.mapper.DeleteMapper;
import work.licht.music.sync.domain.mapper.SelectMapper;
import work.licht.music.sync.domain.mapper.UpdateMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

// 定时分片广播任务：对当日发生变更的帖子点赞数进行同步
@Component
@Slf4j
public class PostLikeCountShardingXxlJob {

    @Resource
    private SelectMapper selectMapper;
    @Resource
    private UpdateMapper updateMapper;
    @Resource
    private DeleteMapper deleteMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 分片广播任务
    @XxlJob("postLikeCountShardingJobHandler")
    public void postLikeCountShardingJobHandler() {
        // 获取分片参数
        // 分片序号
        int shardIndex = XxlJobHelper.getShardIndex();
        // 分片总数
        int shardTotal = XxlJobHelper.getShardTotal();
        XxlJobHelper.log("=================> 开始定时分片广播任务：对当日发生变更的帖子点赞数进行同步");
        XxlJobHelper.log("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);
        log.info("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);
        // 昨日的日期
        String date = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 表名后缀
        String tableNameSuffix = TableConstants.buildTableNameSuffix(date, shardIndex);
        // 一批 1000 条
        int batchSize = 1000;
        // 共同步了多少条记录，默认为 0
        int processedTotal = 0;
        // 死循环
        for (;;) {
            // 1. 分批次查询 t_data_align_post_like_count_temp_日期_分片序号，如一批次查询 1000 条，直到全部查询完成
            List<Long> postIdList = selectMapper.selectBatchFromSyncPostLikeCountTempTable(tableNameSuffix, batchSize);
            // 若记录为空，终止循环
            if (CollUtil.isEmpty(postIdList)) break;
            // 循环这一批发生变更的帖子 ID
            postIdList.forEach(postId -> {
                // 2: 对 t_post_like 关注表执行 count(*) 操作，获取关注总数
                int likeTotal = selectMapper.selectPostLikeCountFromPostLikeTableByUserId(postId);
                // 3: 更新 t_post_count 表, 更新对应 Redis 缓存
                int count = updateMapper.updatePostLikeTotalByUserId(postId, likeTotal);
                // 更新对应 Redis 缓存
                if (count > 0) {
                    String redisKey = RedisKeyConstants.buildCountPostKey(postId);
                    // 判断 Hash 是否存在
                    boolean hashKey = redisTemplate.hasKey(redisKey);
                    // 若存在
                    if (hashKey) {
                        // 更新 Hash 中的 Field 点赞总数
                        redisTemplate.opsForHash().put(redisKey, RedisKeyConstants.FIELD_LIKE_TOTAL, likeTotal);
                    }
                }
            });
            // 4. 批量物理删除这一批次记录
            deleteMapper.batchDeleteSyncPostLikeCountTempTable(tableNameSuffix, postIdList);
            // 当前已处理的记录数
            processedTotal += postIdList.size();
        }
        XxlJobHelper.log("=================> 结束定时分片广播任务：对当日发生变更的帖子点赞数进行同步，共同步记录数：{}", processedTotal);
    }

}