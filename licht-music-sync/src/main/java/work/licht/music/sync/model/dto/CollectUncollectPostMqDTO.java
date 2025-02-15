package work.licht.music.sync.model.dto;

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
public class CollectUncollectPostMqDTO {

    private Long userId;

    private Long postId;

    // 0: 取消收藏， 1：收藏
    private Integer type;

    private LocalDateTime createTime;

    // 帖子发布者 ID
    private Long postCreatorId;

}
