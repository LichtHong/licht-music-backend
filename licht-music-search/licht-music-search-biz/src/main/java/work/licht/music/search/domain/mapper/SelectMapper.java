package work.licht.music.search.domain.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 查询
 */
public interface SelectMapper {

    // 查询帖子文档所需的全字段数据
    List<Map<String, Object>> selectPostIndexData(@Param("postId") Long postId, @Param("userId") Long userId);

    // 查询用户索引所需的全字段数据
    List<Map<String, Object>> selectUserIndexData(@Param("userId") Long userId);

}