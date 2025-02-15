package work.licht.music.post.rpc;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import work.licht.music.common.response.Response;
import work.licht.music.kv.api.KVFeignApi;
import work.licht.music.kv.model.dto.req.AddPostContentReqDTO;
import work.licht.music.kv.model.dto.req.DeletePostContentReqDTO;
import work.licht.music.kv.model.dto.req.FindPostContentReqDTO;
import work.licht.music.kv.model.dto.resp.FindPostContentRespDTO;

import java.util.Objects;

@Component
public class KVRpcService {

    @Resource
    private KVFeignApi kvFeignApi;

    // 保存帖子内容
    public boolean savePostContent(String uuid, String content) {
        AddPostContentReqDTO addPostContentReqDTO = new AddPostContentReqDTO();
        addPostContentReqDTO.setUuid(uuid);
        addPostContentReqDTO.setContent(content);
        Response<?> response = kvFeignApi.addPostContent(addPostContentReqDTO);
        return !Objects.isNull(response) && response.isSuccess();
    }

    // 删除帖子内容
    public boolean deletePostContent(String uuid) {
        DeletePostContentReqDTO deletePostContentReqDTO = new DeletePostContentReqDTO();
        deletePostContentReqDTO.setUuid(uuid);
        Response<?> response = kvFeignApi.deletePostContent(deletePostContentReqDTO);
        return !Objects.isNull(response) && response.isSuccess();
    }

    // 查询帖子内容
    public String findPostContent(String uuid) {
        FindPostContentReqDTO findPostContentReqDTO = new FindPostContentReqDTO();
        findPostContentReqDTO.setUuid(uuid);
        Response<FindPostContentRespDTO> response = kvFeignApi.findPostContent(findPostContentReqDTO);
        if (Objects.isNull(response) || !response.isSuccess() || Objects.isNull(response.getData())) {
            return null;
        }
        return response.getData().getContent();
    }

}