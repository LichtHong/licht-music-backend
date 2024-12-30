package work.licht.music.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StatusEnum {

    ENABLE(0), // 启用
    DISABLED(1); // 禁用

    private final Integer value;

}
