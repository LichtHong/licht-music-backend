package work.licht.music.search.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import work.licht.music.common.enums.StatusEnum;
import work.licht.music.search.config.CanalProperties;
import work.licht.music.search.domain.mapper.SelectMapper;
import work.licht.music.search.enums.PostStatusEnum;
import work.licht.music.search.enums.PostVisibleEnum;
import work.licht.music.search.model.index.PostIndex;
import work.licht.music.search.model.index.UserIndex;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

// Canal 数据消费
@Component
@Slf4j
public class CanalSchedule implements Runnable {

    @Resource
    private CanalProperties canalProperties;
    @Resource
    private CanalConnector canalConnector;
    @Resource
    private RestHighLevelClient restHighLevelClient;
    @Resource
    private SelectMapper selectMapper;

    @Override
    @Scheduled(fixedDelay = 100) // 每隔 100ms 被执行一次
    public void run() {
        // 初始化批次 ID，-1 表示未开始或未获取到数据
        long batchId = -1;
        try {
            // 从 canalConnector 获取批量消息，返回的数据量由 batchSize 控制，若不足，则拉取已有的
            Message message = canalConnector.getWithoutAck(canalProperties.getBatchSize());
            // 获取当前拉取消息的批次 ID
            batchId = message.getId();
            // 获取当前批次中的数据条数
            long size = message.getEntries().size();
            if (batchId == -1 || size == 0) {
                try {
                    // 拉取数据为空，休眠 1s, 防止频繁拉取
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ignored) {
                }
            } else {
                // 如果当前批次有数据，处理这批次数据
                processEntry(message.getEntries());
            }
            // 对当前批次的消息进行 ack 确认，表示该批次的数据已经被成功消费
            canalConnector.ack(batchId);
        } catch (Exception e) {
            log.error("消费 Canal 批次数据异常", e);
            // 如果出现异常，需要进行数据回滚，以便重新消费这批次的数据
            canalConnector.rollback(batchId);
        }
    }

    // 处理这一批次数据
    private void processEntry(List<CanalEntry.Entry> entryList) throws Exception {
        // 循环处理批次数据
        for (CanalEntry.Entry entry : entryList) {
            // 只处理 ROW DATA 行数据类型的 Entry，忽略事务等其他类型
            if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
                // 获取事件类型（如：INSERT、UPDATE、DELETE 等等）
                CanalEntry.EventType eventType = entry.getHeader().getEventType();
                // 获取数据库名称
                String database = entry.getHeader().getSchemaName();
                // 获取表名称
                String table = entry.getHeader().getTableName();
                // 解析出 RowChange 对象，包含 RowData 和事件相关信息
                CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                // 遍历所有行数据（RowData）
                for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                    // 获取行中所有列的最新值（AfterColumns）
                    List<CanalEntry.Column> columns = rowData.getAfterColumnsList();
                    // 将列数据解析为 Map，方便后续处理
                    Map<String, Object> columnMap = parseColumns2Map(columns);
                    log.info("EventType: {}, Database: {}, Table: {}, Columns: {}", eventType, database, table, columnMap);
                    // 处理事件
                    processEvent(columnMap, table, eventType);
                }
            }
        }
    }


    // 将列数据解析为 Map
    private Map<String, Object> parseColumns2Map(List<CanalEntry.Column> columns) {
        Map<String, Object> map = Maps.newHashMap();
        columns.forEach(column -> {
            if (Objects.isNull(column)) return;
            map.put(column.getName(), column.getValue());
        });
        return map;
    }

    // 处理事件
    private void processEvent(Map<String, Object> columnMap, String table, CanalEntry.EventType eventType) throws Exception {
        switch (table) {
            case "t_post" -> handlePostEvent(columnMap, eventType); // 帖子表
            case "t_user" -> handleUserEvent(columnMap, eventType); // 用户表
            default -> log.warn("Table: {} not support", table);
        }
    }

    // 处理帖子表事件
    private void handlePostEvent(Map<String, Object> columnMap, CanalEntry.EventType eventType) throws Exception {
        // 获取帖子 ID
        Long postId = Long.parseLong(columnMap.get("id").toString());
        // 不同的事件，处理逻辑不同
        switch (eventType) {
            case INSERT -> syncPostIndex(postId); // 记录新增事件
            case UPDATE -> {
                // 记录更新事件
                // 帖子变更后的状态
                Integer status = Integer.parseInt(columnMap.get("status").toString());
                // 帖子可见范围
                Integer visible = Integer.parseInt(columnMap.get("visible").toString());
                if (Objects.equals(status, PostStatusEnum.NORMAL.getCode()) && Objects.equals(visible, PostVisibleEnum.PUBLIC.getCode())) {
                    // 正常展示，并且可见性为公开
                    // 对索引进行覆盖更新
                    syncPostIndex(postId);
                } else if (Objects.equals(visible, PostVisibleEnum.PRIVATE.getCode())
                        || Objects.equals(status, PostStatusEnum.DELETED.getCode())
                        || Objects.equals(status, PostStatusEnum.DOWNED.getCode())) {
                    // 仅对自己可见
                    // 被逻辑删除、被下架
                    deletePostDocument(String.valueOf(postId));
                }
            }
            default -> log.info("Unhandled event type for t_post: {}", eventType);
        }
    }

    // 同步帖子索引
    private void syncPostIndex(Long postId) throws Exception {
        // 从数据库查询 Elasticsearch 索引数据
        List<Map<String, Object>> result = selectMapper.selectPostIndexData(postId, null);
        // 遍历查询结果，将每条记录同步到 Elasticsearch
        for (Map<String, Object> recordMap : result) {
            // 创建索引请求对象，指定索引名称
            IndexRequest indexRequest = new IndexRequest(PostIndex.NAME);
            // 设置文档的 ID，使用记录中的主键 “id” 字段值
            indexRequest.id((String.valueOf(recordMap.get(PostIndex.FIELD_POST_ID))));
            // 设置文档的内容，使用查询结果的记录数据
            indexRequest.source(recordMap);
            // 将数据写入 Elasticsearch 索引
            restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
        }
    }

    // 删除指定 ID 的文档
    private void deletePostDocument(String documentId) throws Exception {
        // 创建删除请求对象，指定索引名称和文档 ID
        DeleteRequest deleteRequest = new DeleteRequest(PostIndex.NAME, documentId);
        // 执行删除操作，将指定文档从 Elasticsearch 索引中删除
        restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
    }

    // 处理用户表事件
    private void handleUserEvent(Map<String, Object> columnMap, CanalEntry.EventType eventType) throws Exception {
        // 获取用户 ID
        Long userId = Long.parseLong(columnMap.get("id").toString());
        // 不同的事件，处理逻辑不同
        switch (eventType) {
            case INSERT -> syncUserIndex(userId); // 记录新增事件
            case UPDATE -> {
                // 记录更新事件
                // 用户变更后的状态
                Integer status = Integer.parseInt(columnMap.get("status").toString());
                // 逻辑删除
                Integer isDeleted = Integer.parseInt(columnMap.get("is_deleted").toString());

                if (Objects.equals(status, StatusEnum.ENABLE.getValue()) && Objects.equals(isDeleted, 0)) {
                    // 用户状态为已启用，并且未被逻辑删除
                    // 更新用户索引、帖子索引
                    syncPostIndexAndUserIndex(userId);
                } else if (Objects.equals(status, StatusEnum.DISABLED.getValue()) || Objects.equals(isDeleted, 1)) {
                    // 用户状态为禁用或被逻辑删除
                    // 删除用户文档
                    deleteUserDocument(String.valueOf(userId));
                }
            }
            default -> log.info("Unhandled event type for t_user: {}", eventType);
        }
    }

    // 同步用户索引
    private void syncUserIndex(Long userId) throws Exception {
        // 1. 同步用户索引
        List<Map<String, Object>> userResult = selectMapper.selectUserIndexData(userId);
        // 遍历查询结果，将每条记录同步到 Elasticsearch
        for (Map<String, Object> recordMap : userResult) {
            // 创建索引请求对象，指定索引名称
            IndexRequest indexRequest = new IndexRequest(UserIndex.NAME);
            // 设置文档的 ID，使用记录中的主键 “id” 字段值
            indexRequest.id((String.valueOf(recordMap.get(UserIndex.FIELD_USER_ID))));
            // 设置文档的内容，使用查询结果的记录数据
            indexRequest.source(recordMap);
            // 将数据写入 Elasticsearch 索引
            restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
        }
    }

    // 同步用户索引、帖子索引（可能是多条）
    private void syncPostIndexAndUserIndex(Long userId) throws Exception {
        // 创建一个 BulkRequest
        BulkRequest bulkRequest = new BulkRequest();
        // 1. 用户索引
        List<Map<String, Object>> userResult = selectMapper.selectUserIndexData(userId);
        // 遍历查询结果，将每条记录同步到 Elasticsearch
        for (Map<String, Object> recordMap : userResult) {
            // 创建索引请求对象，指定索引名称
            IndexRequest indexRequest = new IndexRequest(UserIndex.NAME);
            // 设置文档的 ID，使用记录中的主键 “id” 字段值
            indexRequest.id((String.valueOf(recordMap.get(UserIndex.FIELD_USER_ID))));
            // 设置文档的内容，使用查询结果的记录数据
            indexRequest.source(recordMap);
            // 将每个 IndexRequest 加入到 BulkRequest
            bulkRequest.add(indexRequest);
        }
        // 2. 帖子索引
        List<Map<String, Object>> postResult = selectMapper.selectPostIndexData(null, userId);
        for (Map<String, Object> recordMap : postResult) {
            // 创建索引请求对象，指定索引名称
            IndexRequest indexRequest = new IndexRequest(PostIndex.NAME);
            // 设置文档的 ID，使用记录中的主键 “id” 字段值
            indexRequest.id((String.valueOf(recordMap.get(PostIndex.FIELD_POST_ID))));
            // 设置文档的内容，使用查询结果的记录数据
            indexRequest.source(recordMap);
            // 将每个 IndexRequest 加入到 BulkRequest
            bulkRequest.add(indexRequest);
        }
        // 执行批量请求
        restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

    // 删除指定 ID 的用户文档
    private void deleteUserDocument(String documentId) throws Exception {
        // 创建删除请求对象，指定索引名称和文档 ID
        DeleteRequest deleteRequest = new DeleteRequest(UserIndex.NAME, documentId);
        // 执行删除操作，将指定文档从 Elasticsearch 索引中删除
        restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
    }

}