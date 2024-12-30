package work.licht.music.auth.rpc;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import work.licht.music.common.response.Response;
import work.licht.music.user.model.api.UserFeignApi;
import work.licht.music.user.model.dto.req.RegisterUserReqDTO;
import work.licht.music.user.model.dto.req.FindUserByPhoneReqDTO;
import work.licht.music.user.model.dto.req.UpdateUserPasswordReqDTO;
import work.licht.music.user.model.dto.resp.FindUserByPhoneRespDTO;

@Component
public class UserRpcService {

    @Resource
    private UserFeignApi userFeignApi;

    // 用户注册
    public Long registerUser(String phone) {
        RegisterUserReqDTO registerUserReqDTO = new RegisterUserReqDTO();
        registerUserReqDTO.setPhone(phone);
        Response<Long> response = userFeignApi.registerUser(registerUserReqDTO);
        if (!response.isSuccess()) {
            return null;
        }
        return response.getData();
    }

    // 根据手机号查询用户信息
    public FindUserByPhoneRespDTO selectUserByPhone(String phone) {
        FindUserByPhoneReqDTO findUserByPhoneReqDTO = new FindUserByPhoneReqDTO();
        findUserByPhoneReqDTO.setPhone(phone);
        Response<FindUserByPhoneRespDTO> response = userFeignApi.findByPhone(findUserByPhoneReqDTO);
        if (!response.isSuccess()) {
            return null;
        }
        return response.getData();
    }

    // 密码更新
    public void updatePassword(String encodePassword) {
        UpdateUserPasswordReqDTO updateUserPasswordReqDTO = new UpdateUserPasswordReqDTO();
        updateUserPasswordReqDTO.setEncodePassword(encodePassword);
        userFeignApi.updatePassword(updateUserPasswordReqDTO);
    }

}
