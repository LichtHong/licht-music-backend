package work.licht.music.post.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

// 帖子取消收藏：执行 Lua 脚本返回结果
@Getter
@AllArgsConstructor
public enum PostUnCollectLuaResultEnum {

    // 布隆过滤器或者 ZSet 不存在
    NOT_EXIST(-1L),
    // 帖子已收藏
    POST_COLLECTED(1L),
    // 帖子未收藏
    POST_NOT_COLLECTED(0L),
    ;

    private final Long code;

    // 根据类型 code 获取对应的枚举
    public static PostUnCollectLuaResultEnum valueOf(Long code) {
        for (PostUnCollectLuaResultEnum postUnCollectLuaResultEnum : PostUnCollectLuaResultEnum.values()) {
            if (Objects.equals(code, postUnCollectLuaResultEnum.getCode())) {
                return postUnCollectLuaResultEnum;
            }
        }
        return null;
    }

}