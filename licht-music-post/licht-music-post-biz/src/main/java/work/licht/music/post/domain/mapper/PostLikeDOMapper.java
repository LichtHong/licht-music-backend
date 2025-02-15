package work.licht.music.post.domain.mapper;

import org.apache.ibatis.annotations.Param;
import work.licht.music.post.domain.model.PostLikeDO;

import java.util.List;

public interface PostLikeDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(PostLikeDO row);

    int insertSelective(PostLikeDO row);

    // 新增帖子点赞记录，若已存在，则更新帖子点赞记录
    int insertOrUpdate(PostLikeDO postLikeDO);

    PostLikeDO selectByPrimaryKey(Long id);

    int selectCountByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    int selectPostIsLiked(@Param("userId") Long userId, @Param("postId") Long postId);

    List<PostLikeDO> selectLikedByUserIdAndLimit(@Param("userId") Long userId, @Param("limit")  int limit);

    List<PostLikeDO> selectByUserId(@Param("userId") Long userId);

    int updateByPrimaryKeySelective(PostLikeDO row);

    int updateByPrimaryKey(PostLikeDO row);

    // 取消点赞
    int update2UnlikeByUserIdAndPostId(PostLikeDO postLikeDO);

}