package work.licht.music.user.domain.mapper;

import work.licht.music.user.domain.model.PermissionDO;

import java.util.List;

public interface PermissionDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(PermissionDO row);

    int insertSelective(PermissionDO row);

    PermissionDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(PermissionDO row);

    int updateByPrimaryKey(PermissionDO row);

    // 查询 APP 端所有被启用的权限
    List<PermissionDO> selectAppEnabledList();
}