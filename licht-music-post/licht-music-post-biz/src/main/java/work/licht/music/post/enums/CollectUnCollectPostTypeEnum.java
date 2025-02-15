package work.licht.music.post.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

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

}
