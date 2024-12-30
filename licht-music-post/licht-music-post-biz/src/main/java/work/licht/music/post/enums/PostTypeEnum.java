package work.licht.music.post.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public enum PostTypeEnum {

    IMAGE_TEXT(0, "图文"),
    VIDEO(1, "视频");

    private final Integer code;
    private final String description;

    // 类型是否有效
    public static boolean isValid(Integer code) {
        for (PostTypeEnum postTypeEnum : PostTypeEnum.values()) {
            if (Objects.equals(code, postTypeEnum.getCode())) {
                return true;
            }
        }
        return false;
    }

    // 根据类型 code 获取对应的枚举
    public static PostTypeEnum valueOf(Integer code) {
        for (PostTypeEnum postTypeEnum : PostTypeEnum.values()) {
            if (Objects.equals(code, postTypeEnum.getCode())) {
                return postTypeEnum;
            }
        }
        return null;
    }

}