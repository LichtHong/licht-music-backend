package work.licht.music.relation.constant;

public class RedisKeyConstants {

    // 关注列表 KEY 前缀
    private static final String USER_FOLLOWER_KEY_PREFIX = "follower:";
    // 构建关注列表完整的 KEY
    public static String buildUserFollowerKey(Long userId) {
        return USER_FOLLOWER_KEY_PREFIX + userId;
    }

    // 粉丝列表 KEY 前缀
    private static final String USER_FAN_KEY_PREFIX = "fan:";
    // 构建粉丝列表完整的 KEY
    public static String buildUserFanKey(Long userId) {
        return USER_FAN_KEY_PREFIX + userId;
    }

}