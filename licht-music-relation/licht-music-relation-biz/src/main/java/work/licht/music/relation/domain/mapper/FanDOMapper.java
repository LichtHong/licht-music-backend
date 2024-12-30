package work.licht.music.relation.domain.mapper;

import work.licht.music.relation.domain.model.FanDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FanDOMapper {
    int deleteByPrimaryKey(Long id);

    int deleteByUserIdAndFanUserId(@Param("userId")Long userId, @Param("fanUserId")Long fanUserId);

    int insert(FanDO row);

    int insertSelective(FanDO row);

    FanDO selectByPrimaryKey(Long id);

    // 查询记录总数
    long selectCountByUserId(Long userId);

    // 分页查询
    List<FanDO> selectPageListByUserId(@Param("userId") Long userId, @Param("offset") long offset, @Param("limit") long limit);

    List<FanDO> selectListByUserId(@Param("userId")Long userId, @Param("limit") long limit);

    int updateByPrimaryKeySelective(FanDO row);

    int updateByPrimaryKey(FanDO row);
}