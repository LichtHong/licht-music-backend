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
public class ChannelTopicRelDO {
    private Long id;

    private Long channelId;

    private Long topicId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}