package work.licht.music.post.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 帖子
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CollectPostReqVO {

    @NotNull(message = "帖子ID不能为空")
    private Long id;

}