package work.licht.music.post.domain.mapper;

import work.licht.music.post.domain.model.PostDO;

public interface PostDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(PostDO row);

    int insertSelective(PostDO row);

    PostDO selectByPrimaryKey(Long id);

    int selectCountByPostId(Long postId);

    // 查询帖子的发布者用户 ID
    Long selectCreatorIdByPostId(Long postId);

    int updateByPrimaryKeySelective(PostDO row);

    int updateByPrimaryKey(PostDO row);

    int updateVisible(PostDO row);

    int updateIsTop(PostDO row);
}