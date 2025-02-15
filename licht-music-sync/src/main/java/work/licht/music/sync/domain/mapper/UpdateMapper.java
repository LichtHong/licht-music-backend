package work.licht.music.sync.domain.mapper;

import org.apache.ibatis.annotations.Param;

// 更新
public interface UpdateMapper {

    // 更新 t_user_count 计数表总关注数
    int updateUserFollowingTotalByUserId(@Param("userId") long userId, @Param("followingTotal") int followingTotal);
    
    // 更新 t_user_count 计数表总粉丝数
    int updateUserFollowerTotalByUserId(@Param("userId") long userId, @Param("followerTotal") int followerTotal);

    // 更新 t_user_count 计数表获得的总点赞数
    int updateUserLikeTotalByUserId(@Param("userId") long userId, @Param("likeTotal") int likeTotal);

    // 更新 t_user_count 计数表获得的总收藏数
    int updateUserCollectTotalByUserId(@Param("userId") long userId, @Param("collectTotal") int collectTotal);

    // 更新 t_user_count 计数表获得的总帖子发布数
    int updateUserPublishTotalByUserId(@Param("userId") long userId, @Param("publishTotal") int publishTotal);

    // 更新 t_post_count 计数表帖子点赞数
    int updatePostLikeTotalByUserId(@Param("postId") long postId, @Param("postLikeTotal") int postLikeTotal);

    // 更新 t_post_count 计数表帖子收藏数
    int updatePostCollectTotalByUserId(@Param("postId") long postId, @Param("postCollectTotal") int postCollectTotal);

}