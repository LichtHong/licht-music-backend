package work.licht.music.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

// 帖子排序类型
@Getter
@AllArgsConstructor
public enum PostSortTypeEnum {

    // 最新
    LATEST(0),

    // 最多点赞
    MOST_LIKE(1),

    // 最多评论
    MOST_COMMENT(2),

    // 最多收藏
    MOST_COLLECT(3),
    ;

    private final Integer code;

    // 根据类型 code 获取对应的枚举
    public static PostSortTypeEnum valueOf(Integer code) {
        for (PostSortTypeEnum postSortTypeEnum : PostSortTypeEnum.values()) {
            if (Objects.equals(code, postSortTypeEnum.getCode())) {
                return postSortTypeEnum;
            }
        }
        return null;
    }

}