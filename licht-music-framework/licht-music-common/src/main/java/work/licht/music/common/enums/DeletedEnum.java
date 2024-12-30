package work.licht.music.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeletedEnum {

    YES(true), NO(false);

    private final Boolean value;

}