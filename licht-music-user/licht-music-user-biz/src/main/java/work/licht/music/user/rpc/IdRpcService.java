package work.licht.music.user.rpc;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import work.licht.music.id.api.IdFeignApi;

@Component
public class IdRpcService {
    @Resource
    private IdFeignApi idFeignApi;

    // Leaf 号段模式 业务标识
    private static final String BIZ_TAG_LICHT_MUSIC_USER_ID = "leaf-segment-licht-music-user-id";
    // 调用分布式ID生成服务生成 user name id
    public String getUserId() {
        return idFeignApi.getSegmentId(BIZ_TAG_LICHT_MUSIC_USER_ID);
    }

    // Leaf 号段模式 业务标识
    private static final String BIZ_TAG_LICHT_MUSIC_USER_NAME_SUFFIX_ID = "leaf-segment-licht-music-user-name-suffix";
    // 调用分布式ID生成服务生成 user name suffix
    public String getUserNameSuffix() {
        return idFeignApi.getSegmentId(BIZ_TAG_LICHT_MUSIC_USER_NAME_SUFFIX_ID);
    }
}