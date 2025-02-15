package work.licht.music.sync.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import work.licht.music.sync.constant.TableConstants;
import work.licht.music.sync.domain.mapper.DeleteTableMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

//定时任务：自动删除最近一个月的日增量临时表
@Component
@RefreshScope
public class DeleteTableXxlJob {

    // 表总分片数
    @Value("${table.shards}")
    private int tableShards;

    @Resource
    private DeleteTableMapper deleteTableMapper;

    // 自动删除最近一个月的日增量临时表任务
    @XxlJob("deleteTableJobHandler")
    public void deleteTableJobHandler() {
        XxlJobHelper.log("## 开始删除最近一个月的日增量临时表");
        // 今日
        LocalDate today = LocalDate.now();
        // 日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate startDate = today;
        // 从昨天开始往前推一个月
        LocalDate endDate = today.minusMonths(1);
        // 循环最近一个月的日期，不包括今天
        while (startDate.isAfter(endDate)) {
            // 往前推一天
            startDate = startDate.minusDays(1);
            // 日期字符串
            String date = startDate.format(formatter);
            for (int hashKey = 0; hashKey < tableShards; hashKey++) {
                // 表名后缀
                String tableNameSuffix = TableConstants.buildTableNameSuffix(date, hashKey);
                XxlJobHelper.log("删除表后缀: {}", tableNameSuffix);
                // 删除表
                deleteTableMapper.deleteSyncFollowingCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteSyncFollowerCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteSyncPostLikeCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteSyncPostCollectCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteSyncUserPublishCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteSyncUserLikeCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteSyncUserCollectCountTempTable(tableNameSuffix);
            }
        }
        XxlJobHelper.log("## 结束删除最近一个月的日增量临时表");
    }

}
