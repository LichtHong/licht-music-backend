package work.licht.music.count.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 聚合后计数：点赞、取消点赞帖子

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AggregationCountCollectUncollectPostMqDTO {

    // 帖子发布者 ID
    private Long creatorId;

    // 帖子 ID
    private Long postId;

    // 聚合后的计数
    private Integer count;

}
