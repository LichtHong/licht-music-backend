package work.licht.music.post.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

// 帖子取消点赞：执行 Lua 脚本返回结果
@Getter
@AllArgsConstructor
public enum PostUnlikeLuaResultEnum {

    // 布隆过滤器或者 ZSet 不存在
    NOT_EXIST(-1L),
    // 帖子已点赞
    POST_LIKED(1L),
    // 帖子未点赞
    POST_NOT_LIKED(0L),
    ;

    private final Long code;

    // 根据类型code获取对应的枚举
    public static PostUnlikeLuaResultEnum valueOf(Long code) {
        for (PostUnlikeLuaResultEnum postUnlikeLuaResultEnum : PostUnlikeLuaResultEnum.values()) {
            if (Objects.equals(code, postUnlikeLuaResultEnum.getCode())) {
                return postUnlikeLuaResultEnum;
            }
        }
        return null;
    }

}