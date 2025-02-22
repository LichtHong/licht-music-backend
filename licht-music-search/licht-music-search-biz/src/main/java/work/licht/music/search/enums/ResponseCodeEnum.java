package work.licht.music.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import work.licht.music.common.exception.BaseExceptionInterface;

// 响应异常码
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("SEARCH-10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("SEARCH-10001", "参数错误"),

    // ----------- 业务异常状态码 -----------

    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;

}