package work.licht.music.sync.domain.mapper;

// 自动创建表
public interface CreateTableMapper {

    // 创建日增量表：关注数计数变更
    void createSyncFollowingCountTempTable(String tableNameSuffix);

    // 创建日增量表：粉丝数计数变更
    void createSyncFollowerCountTempTable(String tableNameSuffix);

    // 创建日增量表：帖子点赞数计数变更
    void createSyncPostLikeCountTempTable(String tableNameSuffix);

    // 创建日增量表：帖子收藏数计数变更
    void createSyncPostCollectCountTempTable(String tableNameSuffix);

    // 创建日增量表：用户维度帖子发布数计数变更
    void createSyncUserPublishCountTempTable(String tableNameSuffix);

    // 创建日增量表：用户维度帖子被点赞数计数变更
    void createSyncUserLikeCountTempTable(String tableNameSuffix);

    // 创建日增量表：用户维度帖子被收藏数计数变更
    void createSyncUserCollectCountTempTable(String tableNameSuffix);
    
}