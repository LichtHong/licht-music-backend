package work.licht.music.post.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostDO {
    private Long id;

    private String title;

    private Boolean isContentEmpty;

    private Long creatorId;

    private Long topicId;

    private String topicName;

    private Boolean isTop;

    private Integer type;

    private String imgUri;

    private String videoUri;

    private String contentUuid;

    private Integer visible;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer status;
}