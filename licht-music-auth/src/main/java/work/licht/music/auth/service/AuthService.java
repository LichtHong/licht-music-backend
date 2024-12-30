package work.licht.music.auth.service;

import work.licht.music.auth.model.vo.user.UpdatePasswordReqVO;
import work.licht.music.auth.model.vo.user.UserLoginReqVO;
import work.licht.music.common.response.Response;

public interface AuthService {

    Response<String> loginAndRegister(UserLoginReqVO userLoginReqVO);

    // 退出登录
    Response<?> logout();

    // 修改密码
    Response<?> updatePassword(UpdatePasswordReqVO updatePasswordReqVO);
}
