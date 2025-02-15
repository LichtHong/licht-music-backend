package work.licht.music.count.domain.mapper;

import org.apache.ibatis.annotations.Param;
import work.licht.music.count.domain.model.PostCountDO;

public interface PostCountDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(PostCountDO row);

    int insertSelective(PostCountDO row);

    // 添加帖子计数记录或更新帖子点赞数
    int insertOrUpdateLikeTotalByPostId(@Param("count") Integer count, @Param("postId") Long postId);

    // 添加记录或更新帖子收藏数
    int insertOrUpdateCollectTotalByPostId(@Param("count") Integer count, @Param("postId") Long postId);

    PostCountDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(PostCountDO row);

    int updateByPrimaryKey(PostCountDO row);
}