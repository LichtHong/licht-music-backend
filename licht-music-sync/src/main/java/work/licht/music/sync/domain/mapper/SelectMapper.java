package work.licht.music.sync.domain.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

// 查询
public interface SelectMapper {
    
    // 日增量表：关注数计数变更 - 批量查询
    List<Long> selectBatchFromSyncFollowingCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("batchSize") int batchSize);

    // 查询 t_following 关注表，获取关注总数
    int selectFollowingCountFromFollowingTableByUserId(long userId);

    // 日增量表：粉丝数计数变更 - 批量查询
    List<Long> selectBatchFromSyncFollowerCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("batchSize") int batchSize);

    // 查询 t_follower 粉丝表，获取粉丝总数
    int selectFollowerCountFromFollowerTableByUserId(long userId);

    // 日增量表：用户获得的点赞数计数变更 - 批量查询
    List<Long> selectBatchFromSyncUserLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("batchSize") int batchSize);

    // 查询 t_post_like 帖子点赞表，获取用户获得的点赞总数
    int selectUserLikeCountFromPostLikeTableByUserId(long userId);

    // 日增量表：用户获得的收藏数计数变更 - 批量查询
    List<Long> selectBatchFromSyncUserCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("batchSize") int batchSize);

    // 查询 t_post_collection 帖子收藏表，获取用户获得的收藏总数
    int selectUserCollectCountFromPostCollectionTableByUserId(long userId);

    // 日增量表：用户帖子发布数变更 - 批量查询
    List<Long> selectBatchFromSyncUserPublishCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("batchSize") int batchSize);

    // 查询 t_post 帖子表，获取用户发布的帖子总数
    int selectUserPublishCountFromPostTableByUserId(long userId);

    // 日增量表：帖子点赞数变更 - 批量查询
    List<Long> selectBatchFromSyncPostLikeCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("batchSize") int batchSize);

    // 查询 t_post_like 帖子点赞表，获取点赞总数
    int selectPostLikeCountFromPostLikeTableByUserId(long postId);

    // 日增量表：帖子收藏数变更 - 批量查询
    List<Long> selectBatchFromSyncPostCollectCountTempTable(@Param("tableNameSuffix") String tableNameSuffix, @Param("batchSize") int batchSize);

    // 查询 t_post_collection 帖子收藏表，获取收藏总数
    int selectPostCollectCountFromPostCollectionTableByUserId(long postId);

}