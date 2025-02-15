package work.licht.music.count.constant;

public class RedisKeyConstants {

    // 用户维度计数 Key 前缀
    private static final String COUNT_USER_KEY_PREFIX = "count:user:";

    // Hash Field: 粉丝总数
    public static final String FIELD_FOLLOWER_TOTAL = "followerTotal";

    // 构建用户维度计数 Key
    public static String buildCountUserKey(Long userId) {
        return COUNT_USER_KEY_PREFIX + userId;
    }

    // Hash Field: 关注总数
    public static final String FIELD_FOLLOWING_TOTAL = "followingTotal";

    // 帖子维度计数 Key 前缀
    private static final String COUNT_POST_KEY_PREFIX = "count:post:";

    // Hash Field: 帖子点赞总数
    public static final String FIELD_LIKE_TOTAL = "likeTotal";

    // 构建帖子维度计数 Key
    public static String buildCountPostKey(Long postId) {
        return COUNT_POST_KEY_PREFIX + postId;
    }

    // Hash Field: 帖子收藏总数
    public static final String FIELD_COLLECT_TOTAL = "collectTotal";

    // Hash Field: 帖子发布总数
    public static final String FIELD_PUBLISH_TOTAL = "publishTotal";

}