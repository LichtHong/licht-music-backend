package work.licht.music.relation.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindFollowerListReqVO {

    @NotNull(message = "查询用户ID不能为空")
    private Long userId;

    @NotNull(message = "页码不能为空")
    private Integer pageNo = 1; // 默认值为第一页

}
