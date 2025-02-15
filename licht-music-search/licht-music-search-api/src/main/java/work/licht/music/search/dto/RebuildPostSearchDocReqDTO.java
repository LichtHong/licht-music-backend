package work.licht.music.search.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 重建帖子文档
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RebuildPostSearchDocReqDTO {

    @NotNull(message = "帖子 ID 不能为空")
    private Long id;

}