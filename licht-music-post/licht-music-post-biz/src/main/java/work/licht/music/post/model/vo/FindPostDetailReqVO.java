package work.licht.music.post.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 查询帖子详情
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindPostDetailReqVO {

    @NotNull(message = "帖子 ID 不能为空")
    private Long id;

}