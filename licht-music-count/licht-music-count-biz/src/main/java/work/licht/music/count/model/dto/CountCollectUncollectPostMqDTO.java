package work.licht.music.count.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 收藏、取消收藏帖子
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CountCollectUncollectPostMqDTO {

    private Long userId;

    private Long postId;

    // 帖子发布者 ID
    private Long postCreatorId;

    // 0: 取消收藏， 1：收藏
    private Integer type;

    private LocalDateTime createTime;

}
