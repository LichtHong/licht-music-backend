package work.licht.music.sync.domain.mapper;

import org.apache.ibatis.annotations.Param;

// 添加记录
public interface InsertMapper {

    // 帖子点赞数：计数变更
    void insert2SyncPostLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("postId") Long postId);

    // 帖子收藏数：计数变更
    void insert2SyncPostCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("postId") Long postId);

    // 用户获得的点赞数：计数变更
    void insert2SyncUserLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("userId") Long userId);

    // 用户获得的收藏数：计数变更
    void insert2SyncUserCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("userId") Long userId);

    // 用户已发布笔记数：计数变更
    void insert2SyncUserPublishCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("userId") Long userId);

    // 用户关注数：计数变更
    void insert2SyncUserFollowingCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("userId") Long userId);

    // 用户粉丝数：计数变更
    void insert2SyncUserFollowerCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("userId") Long userId);

}