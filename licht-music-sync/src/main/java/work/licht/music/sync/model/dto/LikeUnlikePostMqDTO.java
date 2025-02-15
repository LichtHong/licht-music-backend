package work.licht.music.sync.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 点赞、取消点赞帖子
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LikeUnlikePostMqDTO {

    private Long userId;

    private Long postId;

    // 0: 取消点赞， 1：点赞
    private Integer type;

    // 帖子发布者 ID
    private Long postCreatorId;

    private LocalDateTime createTime;

}