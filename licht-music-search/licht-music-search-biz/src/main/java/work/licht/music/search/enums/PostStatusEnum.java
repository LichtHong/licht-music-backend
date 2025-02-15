package work.licht.music.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 帖子状态
@Getter
@AllArgsConstructor
public enum PostStatusEnum {

    BE_EXAMINE(0), // 待审核
    NORMAL(1), // 正常展示
    DELETED(2), // 被删除
    DOWNED(3), // 被下架
    ;

    private final Integer code;

}