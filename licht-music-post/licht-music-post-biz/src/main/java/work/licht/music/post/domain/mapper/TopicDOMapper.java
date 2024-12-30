package work.licht.music.post.domain.mapper;

import work.licht.music.post.domain.model.TopicDO;

public interface TopicDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(TopicDO row);

    int insertSelective(TopicDO row);

    TopicDO selectByPrimaryKey(Long id);

    String selectNameByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TopicDO row);

    int updateByPrimaryKey(TopicDO row);
}