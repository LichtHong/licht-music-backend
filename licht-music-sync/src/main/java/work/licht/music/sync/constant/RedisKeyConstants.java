package work.licht.music.sync.constant;

public class RedisKeyConstants {

    // 布隆过滤器：日增量变更数据，用户帖子点赞，取消点赞 帖子ID 前缀
    public static final String BLOOM_TODAY_POST_LIKE_POST_ID_LIST_KEY = "bloom:sync:post:like:postId";
    // 构建完整的布隆过滤器：日增量变更数据，用户帖子点赞，取消点赞 帖子ID KEY
    public static String buildBloomPostLikePostIdListKey(String date) {
        return BLOOM_TODAY_POST_LIKE_POST_ID_LIST_KEY + date;
    }
    // 布隆过滤器：日增量变更数据，用户帖子点赞，取消点赞 帖子发布者ID 前缀
    public static final String BLOOM_TODAY_POST_LIKE_USER_ID_LIST_KEY = "bloom:sync:post:like:userId";
    // 构建完整的布隆过滤器：日增量变更数据，用户帖子点赞，取消点赞 帖子发布者ID KEY
    public static String buildBloomPostLikeUserIdListKey(String date) {
        return BLOOM_TODAY_POST_LIKE_USER_ID_LIST_KEY + date;
    }

    // 布隆过滤器：日增量变更数据，用户帖子收藏，取消收藏 帖子ID 前缀
    public static final String BLOOM_TODAY_POST_COLLECT_POST_ID_LIST_KEY = "bloom:sync:post:collect:postId";

    // 构建完整的布隆过滤器：日增量变更数据，用户帖子收藏，取消收藏 帖子ID KEY
    public static String buildBloomUserPostCollectPostIdListKey(String date) {
        return BLOOM_TODAY_POST_COLLECT_POST_ID_LIST_KEY + date;
    }

    // 布隆过滤器：日增量变更数据，用户帖子收藏，取消收藏 帖子发布者ID 前缀
    public static final String BLOOM_TODAY_POST_COLLECT_USER_ID_LIST_KEY = "bloom:sync:post:collect:userId";

    // 构建完整的布隆过滤器：日增量变更数据，用户帖子收藏，取消收藏 帖子发布者ID KEY
    public static String buildBloomUserPostCollectUserIdListKey(String date) {
        return BLOOM_TODAY_POST_COLLECT_USER_ID_LIST_KEY + date;
    }

    // 布隆过滤器：日增量变更数据，用户帖子发布，删除 前缀
    public static final String BLOOM_TODAY_POST_OPERATOR_LIST_KEY = "bloom:sync:post:operator:";
    // 构建完整的布隆过滤器：日增量变更数据，用户帖子发布，删除 KEY
    public static String buildBloomPostOperateListKey(String date) {
        return BLOOM_TODAY_POST_OPERATOR_LIST_KEY + date;
    }

    // 布隆过滤器：日增量变更数据，用户关注数 前缀
    public static final String BLOOM_TODAY_USER_FOLLOWING_LIST_KEY = "bloom:sync:user:following:";
    // 构建完整的布隆过滤器：日增量变更数据，用户关注数 KEY
    public static String buildBloomUserFollowingListKey(String date) {
        return BLOOM_TODAY_USER_FOLLOWING_LIST_KEY + date;
    }

    // 布隆过滤器：日增量变更数据，用户粉丝数 前缀
    public static final String BLOOM_TODAY_USER_FOLLOWER_LIST_KEY = "bloom:sync:user:follower:";
    // 构建完整的布隆过滤器：日增量变更数据，用户粉丝数 KEY
    public static String buildBloomUserFollowerListKey(String date) {
        return BLOOM_TODAY_USER_FOLLOWER_LIST_KEY + date;
    }

    // 用户维度计数 Key 前缀
    private static final String COUNT_USER_KEY_PREFIX = "count:user:";
    // 构建用户维度计数 Key
    public static String buildCountUserKey(Long userId) {
        return COUNT_USER_KEY_PREFIX + userId;
    }
    // Hash Field: 关注总数
    public static final String FIELD_FOLLOWING_TOTAL = "followingTotal";
    // Hash Field: 粉丝总数
    public static final String FIELD_FOLLOWER_TOTAL = "followerTotal";
    // Hash Field: 帖子发布总数
    public static final String FIELD_PUBLISH_TOTAL = "publishTotal";

    // 帖子维度计数 Key 前缀
    private static final String COUNT_POST_KEY_PREFIX = "count:post:";
    // 构建笔记维度计数 Key
    public static String buildCountPostKey(Long postId) {
        return COUNT_POST_KEY_PREFIX + postId;
    }
    // Hash Field: 帖子点赞总数
    public static final String FIELD_LIKE_TOTAL = "likeTotal";
    // Hash Field: 帖子收藏总数
    public static final String FIELD_COLLECT_TOTAL = "collectTotal";

}