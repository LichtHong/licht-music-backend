package work.licht.music.post.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostCollectDO {
    private Long id;

    private Long userId;

    private Long postId;

    private LocalDateTime createTime;

    private Integer status;

}