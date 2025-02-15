package work.licht.music.post.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

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

}