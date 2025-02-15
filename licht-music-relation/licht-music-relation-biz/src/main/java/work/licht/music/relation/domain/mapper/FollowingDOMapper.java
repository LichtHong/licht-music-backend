package work.licht.music.relation.domain.mapper;

import org.apache.ibatis.annotations.Param;
import work.licht.music.relation.domain.model.FollowingDO;

import java.util.List;

public interface FollowingDOMapper {
    int deleteByPrimaryKey(Long id);

    int deleteByUserIdAndFollowingUserId(@Param("userId") Long userId, @Param("unfollowUserId") Long unfollowUserId);

    int insert(FollowingDO row);

    int insertSelective(FollowingDO row);

    FollowingDO selectByPrimaryKey(Long id);

    List<FollowingDO> selectByUserId(Long userId);

    // 查询记录总数
    long selectCountByUserId(Long userId);

    // 分页查询
    List<FollowingDO> selectPageListByUserId(@Param("userId") Long userId, @Param("offset") long offset, @Param("limit") long limit);

    // 查询关注用户列表
    List<FollowingDO> selectAllByUserId(Long userId);

    int updateByPrimaryKeySelective(FollowingDO row);

    int updateByPrimaryKey(FollowingDO row);
}