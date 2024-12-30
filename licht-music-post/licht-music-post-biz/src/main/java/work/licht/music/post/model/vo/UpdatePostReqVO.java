package work.licht.music.post.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdatePostReqVO {
    @NotNull(message = "帖子ID不能为空")
    private Long id;

    @NotNull(message = "帖子类型不能为空")
    private Integer type;

    private List<String> imgUri;

    private String videoUri;

    private String title;

    private String content;

    private Long topicId;
}