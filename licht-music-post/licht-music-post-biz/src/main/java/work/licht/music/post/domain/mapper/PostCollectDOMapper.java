package work.licht.music.post.domain.mapper;

import org.apache.ibatis.annotations.Param;
import work.licht.music.post.domain.model.PostCollectDO;

import java.util.List;

public interface PostCollectDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(PostCollectDO row);

    int insertSelective(PostCollectDO row);

    // 新增帖子收藏记录，若已存在，则更新帖子收藏记录
    int insertOrUpdate(PostCollectDO postCollectDO);

    PostCollectDO selectByPrimaryKey(Long id);

    // 查询帖子是否被收藏
    int selectCountByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    // 查询用户已收藏的帖子
    List<PostCollectDO> selectByUserId(Long userId);

    // 查询用户最近收藏的帖子
    List<PostCollectDO> selectCollectedByUserIdAndLimit(@Param("userId") Long userId, @Param("limit")  int limit);

    int updateByPrimaryKeySelective(PostCollectDO row);

    int updateByPrimaryKey(PostCollectDO row);

    int update2UnCollectByUserIdAndPostId(PostCollectDO postCollectDO);
}