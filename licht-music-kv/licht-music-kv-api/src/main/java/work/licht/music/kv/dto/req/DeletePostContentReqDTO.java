package work.licht.music.kv.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeletePostContentReqDTO {

    @NotBlank(message = "帖子内容UUID不能为空")
    private String uuid;

}