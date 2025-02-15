package work.licht.music.post.constant;

public class RedisKeyConstants {

    // 帖子详情 KEY 前缀
    public static final String POST_DETAIL_KEY = "post:detail:";
    
    // 构建完整的帖子详情 KEY
    public static String buildPostDetailKey(Long postId) {
        return POST_DETAIL_KEY + postId;
    }

    // 布隆过滤器：用户帖子点赞
    public static final String BLOOM_USER_POST_LIKE_LIST_KEY = "bloom:post:like:";

    // 构建完整的布隆过滤器：用户帖子点赞 KEY
    public static String buildBloomUserPostLikeListKey(Long userId) {
        return BLOOM_USER_POST_LIKE_LIST_KEY + userId;
    }

    // 用户帖子点赞列表 ZSet 前缀
    public static final String USER_POST_LIKE_ZSET_KEY = "user:post:like:";

    // 构建完整的用户帖子点赞列表 ZSet KEY
    public static String buildUserPostLikeZSetKey(Long userId) {
        return USER_POST_LIKE_ZSET_KEY + userId;
    }

    // 布隆过滤器：用户帖子收藏 前缀
    public static final String BLOOM_USER_POST_COLLECT_LIST_KEY = "bloom:post:collect:";

    // 构建完整的布隆过滤器：用户帖子收藏 KEY
    public static String buildBloomUserPostCollectListKey(Long userId) {
        return BLOOM_USER_POST_COLLECT_LIST_KEY + userId;
    }

    // 用户帖子收藏列表 ZSet 前缀
    public static final String USER_POST_COLLECT_ZSET_KEY = "user:post:collect:";

    // 构建完整的用户帖子收藏列表 ZSet KEY
    public static String buildUserPostCollectZSetKey(Long userId) {
        return USER_POST_COLLECT_ZSET_KEY + userId;
    }

}
