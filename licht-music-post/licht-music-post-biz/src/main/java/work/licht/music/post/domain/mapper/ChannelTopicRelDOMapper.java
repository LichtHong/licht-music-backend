package work.licht.music.post.domain.mapper;

import work.licht.music.post.domain.model.ChannelTopicRelDO;

public interface ChannelTopicRelDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ChannelTopicRelDO row);

    int insertSelective(ChannelTopicRelDO row);

    ChannelTopicRelDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ChannelTopicRelDO row);

    int updateByPrimaryKey(ChannelTopicRelDO row);
}