package work.licht.music.sync.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import work.licht.music.sync.constant.TableConstants;
import work.licht.music.sync.domain.mapper.CreateTableMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// 定时任务：自动创建日增量计数变更表
@Component
public class CreateTableXxlJob {

    // 表总分片数
    @Value("${table.shards}")
    private int tableShards;
    @Resource
    private CreateTableMapper createTableMapper;

    // 创建计数日增量变更表任务
    @XxlJob("createTableJobHandler")
    public void createTableJobHandler() {
        // 表后缀 明日的日期
        String date = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        XxlJobHelper.log("## 开始创建日增量数据表，日期: {}...", date);
        if (tableShards > 0) {
            for (int hashKey = 0; hashKey < tableShards; hashKey++) {
                // 表名后缀
                String tableNameSuffix = TableConstants.buildTableNameSuffix(date, hashKey);
                // 创建表
                createTableMapper.createSyncFollowingCountTempTable(tableNameSuffix);
                createTableMapper.createSyncFollowerCountTempTable(tableNameSuffix);
                createTableMapper.createSyncPostLikeCountTempTable(tableNameSuffix);
                createTableMapper.createSyncPostCollectCountTempTable(tableNameSuffix);
                createTableMapper.createSyncUserPublishCountTempTable(tableNameSuffix);
                createTableMapper.createSyncUserLikeCountTempTable(tableNameSuffix);
                createTableMapper.createSyncUserCollectCountTempTable(tableNameSuffix);
            }
        }
        XxlJobHelper.log("## 结束创建日增量数据表，日期: {}...", date);
    }

}