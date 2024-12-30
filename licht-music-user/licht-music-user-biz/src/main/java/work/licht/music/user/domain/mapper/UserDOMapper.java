package work.licht.music.user.domain.mapper;

import org.apache.ibatis.annotations.Param;
import work.licht.music.user.domain.model.UserDO;

import java.util.List;

public interface UserDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserDO row);

    int insertSelective(UserDO row);

    UserDO selectByPrimaryKey(Long id);

    UserDO selectByPhone(String phone);

    // 批量查询用户信息
    List<UserDO> selectByIds(@Param("ids") List<Long> ids);

    int updateByPrimaryKeySelective(UserDO row);

    int updateByPrimaryKey(UserDO row);
}