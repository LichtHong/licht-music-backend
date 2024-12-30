package work.licht.music.post.rpc;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import work.licht.music.common.response.Response;
import work.licht.music.user.model.api.UserFeignApi;
import work.licht.music.user.model.dto.req.FindUserByIdReqDTO;
import work.licht.music.user.model.dto.resp.FindUserByIdRespDTO;

import java.util.Objects;

// 用户服务
@Component
public class UserRpcService {

    @Resource
    private UserFeignApi userFeignApi;

    // 查询用户信息
    public FindUserByIdRespDTO findById(Long userId) {
        FindUserByIdReqDTO findUserByIdReqDTO = new FindUserByIdReqDTO();
        findUserByIdReqDTO.setId(userId);
        Response<FindUserByIdRespDTO> response = userFeignApi.findById(findUserByIdReqDTO);
        if (Objects.isNull(response) || !response.isSuccess()) {
            return null;
        }
        return response.getData();
    }

}