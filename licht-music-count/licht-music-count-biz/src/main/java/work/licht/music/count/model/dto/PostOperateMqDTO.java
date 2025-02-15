package work.licht.music.count.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 帖子操作
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostOperateMqDTO {

    // 帖子发布者 ID
    private Long creatorId;

    // 帖子 ID
    private Long postId;

    // 操作类型： 0 - 帖子删除； 1：帖子发布；
    private Integer type;

}
