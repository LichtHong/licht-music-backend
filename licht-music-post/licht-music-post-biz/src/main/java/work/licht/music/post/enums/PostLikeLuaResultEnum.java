package work.licht.music.post.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

// 帖子点赞：执行 Lua 脚本返回结果
@Getter
@AllArgsConstructor
public enum PostLikeLuaResultEnum {

    // 布隆过滤器或者 ZSet 不存在
    NOT_EXIST(-1L),
    // 帖子点赞成功
    POST_LIKE_SUCCESS(0L),
    // 帖子已点赞
    POST_LIKED(1L),
    ;

    private final Long code;

    // 根据类型 code 获取对应的枚举
    public static PostLikeLuaResultEnum valueOf(Long code) {
        for (PostLikeLuaResultEnum postLikeLuaResultEnum : PostLikeLuaResultEnum.values()) {
            if (Objects.equals(code, postLikeLuaResultEnum.getCode())) {
                return postLikeLuaResultEnum;
            }
        }
        return null;
    }
}
