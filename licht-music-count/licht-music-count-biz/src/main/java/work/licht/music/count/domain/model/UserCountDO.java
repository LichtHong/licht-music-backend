package work.licht.music.count.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserCountDO {
    private Long id;

    private Long userId;

    private Long followerTotal;

    private Long followingTotal;

    private Long postTotal;

    private Long likeTotal;

    private Long collectTotal;

}