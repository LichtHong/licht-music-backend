package work.licht.music.count.domain.mapper;

import org.apache.ibatis.annotations.Param;
import work.licht.music.count.domain.model.UserCountDO;

public interface UserCountDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserCountDO row);

    int insertSelective(UserCountDO row);

    int insertOrUpdateFollowerTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

    int insertOrUpdateLikeTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

    int insertOrUpdateCollectTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

    // 添加记录或更新帖子发布数
    int insertOrUpdatePublishTotalByUserId(@Param("count") Long count, @Param("userId") Long userId);

    UserCountDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserCountDO row);

    int updateByPrimaryKey(UserCountDO row);
}