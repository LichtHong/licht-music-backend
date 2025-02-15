package work.licht.music.relation.domain.mapper;

import work.licht.music.relation.domain.model.FollowerDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FollowerDOMapper {
    int deleteByPrimaryKey(Long id);

    int deleteByUserIdAndFollowerUserId(@Param("userId")Long userId, @Param("followerUserId")Long followerUserId);

    int insert(FollowerDO row);

    int insertSelective(FollowerDO row);

    FollowerDO selectByPrimaryKey(Long id);

    // 查询记录总数
    long selectCountByUserId(Long userId);

    // 分页查询
    List<FollowerDO> selectPageListByUserId(@Param("userId") Long userId, @Param("offset") long offset, @Param("limit") long limit);

    List<FollowerDO> selectListByUserId(@Param("userId")Long userId, @Param("limit") long limit);

    int updateByPrimaryKeySelective(FollowerDO row);

    int updateByPrimaryKey(FollowerDO row);
}