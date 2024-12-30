package work.licht.music.post.constant;

public class RedisKeyConstants {

    // 帖子详情 KEY 前缀
    public static final String POST_DETAIL_KEY = "post:detail:";
    
    // 构建完整的帖子详情 KEY
    public static String buildPostDetailKey(Long postId) {
        return POST_DETAIL_KEY + postId;
    }

}
