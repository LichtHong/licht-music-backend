package work.licht.music.relation.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindFollowerUserRespVO {

    private Long userId;

    private String avatar;

    private String nickname;

    private String introduction;

}
