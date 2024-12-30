package work.licht.music.relation.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 关注用户
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowUserMqDTO {

    private Long userId;

    private Long followUserId;

    private LocalDateTime createTime;
}
