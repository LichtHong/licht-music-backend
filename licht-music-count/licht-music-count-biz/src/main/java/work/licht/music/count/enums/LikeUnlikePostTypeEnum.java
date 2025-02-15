package work.licht.music.count.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

// 帖子点赞、取消点赞 Type
@Getter
@AllArgsConstructor
public enum LikeUnlikePostTypeEnum {
    // 点赞
    LIKE(1),
    // 取消点赞
    UNLIKE(0),
    ;

    private final Integer code;

    public static LikeUnlikePostTypeEnum valueOf(Integer code) {
        for (LikeUnlikePostTypeEnum likeUnlikePostTypeEnum : LikeUnlikePostTypeEnum.values()) {
            if (Objects.equals(code, likeUnlikePostTypeEnum.getCode())) {
                return likeUnlikePostTypeEnum;
            }
        }
        return null;
    }

}