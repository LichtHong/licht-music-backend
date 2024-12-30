package work.licht.music.user.domain.mapper;

import work.licht.music.user.domain.model.UserRoleDO;

public interface UserRoleDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserRoleDO row);

    int insertSelective(UserRoleDO row);

    UserRoleDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserRoleDO row);

    int updateByPrimaryKey(UserRoleDO row);
}