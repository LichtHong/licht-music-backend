package work.licht.music.sync.domain.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

// 删除
public interface DeleteMapper {

    // 日增量表：关注数计数变更 - 批量删除
    void batchDeleteSyncFollowingCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("userIds") List<Long> userIds);

    // 日增量表：粉丝数计数变更 - 批量删除
    void batchDeleteSyncFollowerCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("userIds") List<Long> userIds);

    // 日增量表：用户发布笔记数变更 - 批量删除
    void batchDeleteSyncUserPublishCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("userIds") List<Long> userIds);

    // 日增量表：用户获得的点赞数变更 - 批量删除
    void batchDeleteSyncUserLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("userIds") List<Long> userIds);

    // 日增量表：用户获得的收藏数变更 - 批量删除
    void batchDeleteSyncUserCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("userIds") List<Long> userIds);

    // 日增量表：笔记点赞计数变更 - 批量删除
    void batchDeleteSyncPostLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("postIds") List<Long> postIds);

    // 日增量表：笔记收藏计数变更 - 批量删除
    void batchDeleteSyncPostCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("postIds") List<Long> postIds);
    
}