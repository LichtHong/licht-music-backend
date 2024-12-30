package work.licht.music.relation.domain.mapper;

import org.apache.ibatis.annotations.Param;
import work.licht.music.relation.domain.model.FollowerDO;

import java.util.List;

public interface FollowerDOMapper {
    int deleteByPrimaryKey(Long id);

    int deleteByUserIdAndFollowerUserId(@Param("userId") Long userId, @Param("unfollowUserId") Long unfollowUserId);

    int insert(FollowerDO row);

    int insertSelective(FollowerDO row);

    FollowerDO selectByPrimaryKey(Long id);

    List<FollowerDO> selectByUserId(Long userId);

    // 查询记录总数
    long selectCountByUserId(Long userId);

    // 分页查询
    List<FollowerDO> selectPageListByUserId(@Param("userId") Long userId, @Param("offset") long offset, @Param("limit") long limit);

    // 查询关注用户列表
    List<FollowerDO> selectAllByUserId(Long userId);

    int updateByPrimaryKeySelective(FollowerDO row);

    int updateByPrimaryKey(FollowerDO row);
}