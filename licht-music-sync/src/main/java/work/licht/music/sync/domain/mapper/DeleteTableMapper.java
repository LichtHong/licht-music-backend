package work.licht.music.sync.domain.mapper;

// 自动删除表
public interface DeleteTableMapper {

    // 删除日增量表：关注数计数变更
    void deleteSyncFollowingCountTempTable(String tableNameSuffix);

    // 删除日增量表：粉丝数计数变更
    void deleteSyncFollowerCountTempTable(String tableNameSuffix);

    // 删除日增量表：帖子点赞数计数变更
    void deleteSyncPostLikeCountTempTable(String tableNameSuffix);

    // 删除日增量表：帖子收藏数计数变更
    void deleteSyncPostCollectCountTempTable(String tableNameSuffix);

    // 删除日增量表：用户维度帖子发布数计数变更
    void deleteSyncUserPublishCountTempTable(String tableNameSuffix);

    // 删除日增量表：用户维度帖子被点赞数计数变更
    void deleteSyncUserLikeCountTempTable(String tableNameSuffix);

    // 删除日增量表：用户维度帖子被收藏数计数变更
    void deleteSyncUserCollectCountTempTable(String tableNameSuffix);
    
}