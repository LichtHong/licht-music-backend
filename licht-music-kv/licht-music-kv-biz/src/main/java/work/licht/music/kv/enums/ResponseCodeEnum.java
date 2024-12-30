package work.licht.music.kv.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import work.licht.music.common.exception.BaseExceptionInterface;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("KV-10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("KV-10001", "参数错误"),

    // ----------- 业务异常状态码 -----------
    POST_CONTENT_NOT_FOUND("KV-20000", "该帖子内容不存在"),

    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;

}