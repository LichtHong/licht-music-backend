package work.licht.music.kv.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindPostContentRespDTO {

    // 帖子ID
    private UUID uuid;

    // 帖子内容
    private String content;

}