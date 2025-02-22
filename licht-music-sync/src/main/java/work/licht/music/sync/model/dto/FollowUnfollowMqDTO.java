package work.licht.music.sync.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 用户关注、取关
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowUnfollowMqDTO {

    // 原用户
    private Long userId;

    // 目标用户
    private Long targetUserId;

    // 1:关注 0:取关
    private Integer type;

}
