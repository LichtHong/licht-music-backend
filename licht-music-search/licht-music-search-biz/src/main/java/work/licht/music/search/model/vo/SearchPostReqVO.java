package work.licht.music.search.model.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 搜索帖子
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchPostReqVO {

    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;

    @Min(value = 1, message = "页码不能小于 1")
    private Integer currentPage = 1; // 默认值为第一页

    // 排序：null：综合 / 0：最新 / 1：最多点赞 / 2：最多评论 / 3：最多收藏
    private Integer sort;

    // 发布时间范围：null：不限 / 0：一天内 / 1：一周内 / 2：半年内
    private Integer publishTimeRange;

}