package work.licht.music.user.model.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import work.licht.music.common.response.Response;
import work.licht.music.user.model.constant.ApiConstants;
import work.licht.music.user.model.dto.req.*;
import work.licht.music.user.model.dto.resp.FindUserByIdRespDTO;
import work.licht.music.user.model.dto.resp.FindUserByPhoneRespDTO;

import java.util.List;

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface UserFeignApi {

    String PREFIX = "/user";

    // 用户注册
    @PostMapping(value = PREFIX + "/register")
    Response<Long> registerUser(@RequestBody RegisterUserReqDTO registerUserReqDTO);

    // 根据Id查询用户信息
    @PostMapping(value = PREFIX + "/findById")
    Response<FindUserByIdRespDTO> findById(@RequestBody FindUserByIdReqDTO findUserByIdReqDTO);

    // 根据手机号查询用户信息
    @PostMapping(value = PREFIX + "/findByPhone")
    Response<FindUserByPhoneRespDTO> findByPhone(@RequestBody FindUserByPhoneReqDTO findUserByPhoneReqDTO);

    // 批量查询用户信息
    @PostMapping(value = PREFIX + "/findByIds")
    Response<List<FindUserByIdRespDTO>> findByIds(@RequestBody FindUsersByIdsReqDTO findUsersByIdsReqDTO);

    // 更新密码
    @PostMapping(value = PREFIX + "/password/update")
    Response<?> updatePassword(@RequestBody UpdateUserPasswordReqDTO updateUserPasswordReqDTO);

}