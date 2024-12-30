package work.licht.music.user.service;

import work.licht.music.common.response.Response;
import work.licht.music.user.model.dto.req.*;
import work.licht.music.user.model.dto.resp.FindUserByIdRespDTO;
import work.licht.music.user.model.dto.resp.FindUserByPhoneRespDTO;
import work.licht.music.user.model.vo.UpdateUserInfoReqVO;

import java.util.List;

public interface UserService {

    // 更新用户信息
    Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO);

    // 用户注册
    Response<Long> register(RegisterUserReqDTO registerUserReqDTO);

    // 根据用户 ID 查询用户信息
    Response<FindUserByIdRespDTO> findById(FindUserByIdReqDTO findUserByIdReqDTO);

    // 根据手机号查询用户信息
    Response<FindUserByPhoneRespDTO> findByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO);

    // 批量根据用户 ID 查询用户信息
    Response<List<FindUserByIdRespDTO>> findByIds(FindUsersByIdsReqDTO findUsersByIdsReqDTO);

    // 更新密码
    Response<?> updatePassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO);
}