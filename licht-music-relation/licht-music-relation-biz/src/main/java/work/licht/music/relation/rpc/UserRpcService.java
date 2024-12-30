package work.licht.music.relation.rpc;

import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import work.licht.music.common.response.Response;
import work.licht.music.user.model.api.UserFeignApi;
import work.licht.music.user.model.dto.req.FindUserByIdReqDTO;
import work.licht.music.user.model.dto.req.FindUsersByIdsReqDTO;
import work.licht.music.user.model.dto.resp.FindUserByIdRespDTO;

import java.util.List;
import java.util.Objects;

@Component
public class UserRpcService {

    @Resource
    private UserFeignApi userFeignApi;

    // 根据用户 ID 查询
    public FindUserByIdRespDTO findById(Long userId) {
        FindUserByIdReqDTO findUserByIdReqDTO = new FindUserByIdReqDTO();
        findUserByIdReqDTO.setId(userId);
        Response<FindUserByIdRespDTO> response = userFeignApi.findById(findUserByIdReqDTO);
        if (!response.isSuccess() || Objects.isNull(response.getData())) {
            return null;
        }
        return response.getData();
    }

    public List<FindUserByIdRespDTO> findByIds(List<Long> userIds) {
        FindUsersByIdsReqDTO findUsersByIdsReqDTO = new FindUsersByIdsReqDTO();
        findUsersByIdsReqDTO.setIds(userIds);
        Response<List<FindUserByIdRespDTO>> response = userFeignApi.findByIds(findUsersByIdsReqDTO);
        if (!response.isSuccess() || Objects.isNull(response.getData()) || CollUtil.isEmpty(response.getData())) return null;
        return response.getData();
    }


}