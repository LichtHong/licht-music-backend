package work.licht.music.relation.constant;

public class RedisKeyConstants {

    // 关注列表 KEY 前缀
    private static final String USER_FOLLOWING_KEY_PREFIX = "following:";
    // 构建关注列表完整的 KEY
    public static String buildUserFollowingKey(Long userId) {
        return USER_FOLLOWING_KEY_PREFIX + userId;
    }

    // 粉丝列表 KEY 前缀
    private static final String USER_FOLLOWER_KEY_PREFIX = "follower:";
    // 构建粉丝列表完整的 KEY
    public static String buildUserFollowerKey(Long userId) {
        return USER_FOLLOWER_KEY_PREFIX + userId;
    }

}