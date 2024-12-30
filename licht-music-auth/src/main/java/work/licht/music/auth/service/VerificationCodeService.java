package work.licht.music.auth.service;

import work.licht.music.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import work.licht.music.common.response.Response;

public interface VerificationCodeService {

    // 发送短信验证码
    Response<?> send(SendVerificationCodeReqVO sendVerificationCodeReqVO);

}
