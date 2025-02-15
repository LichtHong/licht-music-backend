package work.licht.music.search.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import work.licht.music.common.response.Response;
import work.licht.music.search.constant.ApiConstants;
import work.licht.music.search.dto.RebuildPostSearchDocReqDTO;
import work.licht.music.search.dto.RebuildUserSearchDocReqDTO;

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface SearchFeignApi {

    String PREFIX = "/search";

    // 重建帖子文档
    @PostMapping(value = PREFIX + "/post/doc/rebuild")
    Response<?> rebuildPostSearchDocument(@RequestBody RebuildPostSearchDocReqDTO rebuildPostSearchDocReqDTO);


    // 重建用户文档
    @PostMapping(value = PREFIX + "/user/doc/rebuild")
    Response<?> rebuildUserSearchDocument(@RequestBody RebuildUserSearchDocReqDTO rebuildUserSearchDocReqDTO);

}
