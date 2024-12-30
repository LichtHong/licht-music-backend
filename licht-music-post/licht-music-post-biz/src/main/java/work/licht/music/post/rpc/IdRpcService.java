package work.licht.music.post.rpc;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import work.licht.music.id.api.IdFeignApi;

@Component
public class IdRpcService {

    @Resource
    private IdFeignApi idFeignApi;

    private static final String BIZ_TAG_LICHT_MUSIC_POST_ID = "leaf-snowflake-licht-music-post-id";
    // 生成 ID 雪花算法
    public String getPostId() {
        return idFeignApi.getSnowflakeId(BIZ_TAG_LICHT_MUSIC_POST_ID);
    }

}