package work.licht.music.post.domain.mapper;

import work.licht.music.post.domain.model.ChannelDO;

public interface ChannelDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ChannelDO row);

    int insertSelective(ChannelDO row);

    ChannelDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ChannelDO row);

    int updateByPrimaryKey(ChannelDO row);
}