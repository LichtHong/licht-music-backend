package work.licht.music.post.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 帖子操作
@Getter
@AllArgsConstructor
public enum PostOperateEnum {

    // 帖子发布
    PUBLISH(1),
    // 帖子删除
    DELETE(0),
    ;

    private final Integer code;

}
