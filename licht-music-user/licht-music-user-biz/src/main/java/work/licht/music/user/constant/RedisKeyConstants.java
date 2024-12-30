package work.licht.music.user.constant;

public class RedisKeyConstants {

    // 全局 ID 生成器 KEY
    public static final String USER_ID_GENERATOR_KEY = "user.id.generator";

    // 用户角色数据 KEY 前缀
    private static final String USER_ROLES_KEY_PREFIX = "user:roles:";
    // 构建用户角色数据 KEY
    public static String buildUserRoleKey(Long userId) {
        return USER_ROLES_KEY_PREFIX + userId;
    }

    // 角色对应的权限集合 KEY 前缀
    private static final String ROLE_PERMISSIONS_KEY_PREFIX = "role:permissions:";
    // 构建角色对应的权限集合 KEY
    public static String buildRolePermissionsKey(String roleKey) {
        return ROLE_PERMISSIONS_KEY_PREFIX + roleKey;
    }

    // 用户信息数据 KEY 前缀
    private static final String USER_INFO_KEY_PREFIX = "user:info:";
    // 构建角色对应的信息数据 KEY
    public static String buildUserInfoKey(Long userId) {
        return USER_INFO_KEY_PREFIX + userId;
    }

}