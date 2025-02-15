package work.licht.music.count.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

// 帖子收藏、取消收藏 Type
@Getter
@AllArgsConstructor
public enum CollectUnCollectPostTypeEnum {

    // 收藏
    COLLECT(1),
    // 取消收藏
    UN_COLLECT(0),
    ;

    private final Integer code;

    public static CollectUnCollectPostTypeEnum valueOf(Integer code) {
        for (CollectUnCollectPostTypeEnum collectUnCollectPostTypeEnum : CollectUnCollectPostTypeEnum.values()) {
            if (Objects.equals(code, collectUnCollectPostTypeEnum.getCode())) {
                return collectUnCollectPostTypeEnum;
            }
        }
        return null;
    }

}

