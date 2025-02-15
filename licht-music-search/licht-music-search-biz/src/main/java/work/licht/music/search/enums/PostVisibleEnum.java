package work.licht.music.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 帖子可见性
@Getter
@AllArgsConstructor
public enum PostVisibleEnum {

    PUBLIC(0), // 公开，所有人可见
    PRIVATE(1); // 仅自己可见

    private final Integer code;

}