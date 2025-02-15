package work.licht.music.kv.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import work.licht.music.common.response.Response;
import work.licht.music.kv.constant.ApiConstants;
import work.licht.music.kv.model.dto.req.AddPostContentReqDTO;
import work.licht.music.kv.model.dto.req.DeletePostContentReqDTO;
import work.licht.music.kv.model.dto.req.FindPostContentReqDTO;
import work.licht.music.kv.model.dto.resp.FindPostContentRespDTO;

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface KVFeignApi {

    String PREFIX = "/kv";

    @PostMapping(value = PREFIX + "/post/content/add")
    Response<?> addPostContent(@RequestBody AddPostContentReqDTO postContentReqDTO);

    @PostMapping(value = PREFIX + "/post/content/find")
    Response<FindPostContentRespDTO> findPostContent(@RequestBody FindPostContentReqDTO findPostContentReqDTO);

    @PostMapping(value = PREFIX + "/post/content/delete")
    Response<?> deletePostContent(@RequestBody DeletePostContentReqDTO deletePostContentReqDTO);

}