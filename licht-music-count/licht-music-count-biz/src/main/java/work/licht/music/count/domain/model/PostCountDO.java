package work.licht.music.count.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostCountDO {
    private Long id;

    private Long postId;

    private Long likeTotal;

    private Long collectTotal;

    private Long commentTotal;

}