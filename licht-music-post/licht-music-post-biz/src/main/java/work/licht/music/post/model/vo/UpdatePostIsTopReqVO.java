package work.licht.music.post.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdatePostIsTopReqVO {

    @NotNull(message = "帖子ID不能为空")
    private Long id;

    @NotNull(message = "置顶状态不能为空")
    private Boolean isTop;

}