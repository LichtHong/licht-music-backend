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
public class FanDO {
    private Long id;

    private Long userId;

    private Long fanUserId;

    private LocalDateTime createTime;
}