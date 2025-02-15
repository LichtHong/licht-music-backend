package work.licht.music.relation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowerDO {

    private Long id;

    private Long userId;

    private Long followerUserId;

    private LocalDateTime createTime;

}