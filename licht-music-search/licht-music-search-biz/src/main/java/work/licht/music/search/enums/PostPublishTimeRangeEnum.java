package work.licht.music.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

// 帖子发布时间范围
@Getter
@AllArgsConstructor
public enum PostPublishTimeRangeEnum {

    // 一天内
    DAY(0),
    // 一周内
    WEEK(1),
    // 半年内
    HALF_YEAR(2),
    ;

    private final Integer code;

    // 根据类型 code 获取对应的枚举
    public static PostPublishTimeRangeEnum valueOf(Integer code) {
        for (PostPublishTimeRangeEnum postPublishTimeRangeEnum : PostPublishTimeRangeEnum.values()) {
            if (Objects.equals(code, postPublishTimeRangeEnum.getCode())) {
                return postPublishTimeRangeEnum;
            }
        }
        return null;
    }

}
