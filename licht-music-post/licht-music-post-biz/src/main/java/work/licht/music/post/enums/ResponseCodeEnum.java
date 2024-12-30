package work.licht.music.post.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import work.licht.music.common.exception.BaseExceptionInterface;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("POST-10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("POST-10001", "参数错误"),

    // ----------- 业务异常状态码 -----------
    POST_TYPE_ERROR("POST-20000", "未知的帖子类型"),
    POST_PUBLISH_FAIL("POST-20001", "帖子发布失败"),
    POST_NOT_FOUND("POST-20002", "帖子不存在"),
    POST_PRIVATE("POST-20003", "作者已将该帖子设置为仅自己可见"),
    POST_UPDATE_FAIL("POST-20004", "帖子更新失败"),
    TOPIC_NOT_FOUND("POST-20005", "话题不存在"),
    POST_CANT_OPERATE("POST-20007", "您无法操作该帖子"),
    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;

}