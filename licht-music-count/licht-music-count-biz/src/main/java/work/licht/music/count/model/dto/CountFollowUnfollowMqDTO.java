package work.licht.music.count.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CountFollowUnfollowMqDTO {

    // 用户
    private Long userId;

    // 目标用户
    private Long targetUserId;

    // 1:关注 0:取关
    private Integer type;

}