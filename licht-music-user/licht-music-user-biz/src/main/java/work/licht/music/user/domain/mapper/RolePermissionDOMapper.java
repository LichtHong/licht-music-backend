package work.licht.music.user.domain.mapper;

import org.apache.ibatis.annotations.Param;
import work.licht.music.user.domain.model.RolePermissionDO;

import java.util.List;

public interface RolePermissionDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(RolePermissionDO row);

    int insertSelective(RolePermissionDO row);

    RolePermissionDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(RolePermissionDO row);

    int updateByPrimaryKey(RolePermissionDO row);

    // 根据角色 ID 集合批量查询
    List<RolePermissionDO> selectByRoleIds(@Param("roleIds") List<Long> roleIds);
}