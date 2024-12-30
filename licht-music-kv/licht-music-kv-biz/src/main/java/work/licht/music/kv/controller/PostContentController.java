package work.licht.music.kv.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import work.licht.music.common.response.Response;
import work.licht.music.kv.dto.req.AddPostContentReqDTO;
import work.licht.music.kv.dto.req.DeletePostContentReqDTO;
import work.licht.music.kv.dto.req.FindPostContentReqDTO;
import work.licht.music.kv.dto.resp.FindPostContentRespDTO;
import work.licht.music.kv.service.PostContentService;

@RestController
@RequestMapping("/kv")
@Slf4j
public class PostContentController {

    @Resource
    private PostContentService postContentService;

    @PostMapping(value = "/post/content/add")
    public Response<?> addPostContent(@Validated @RequestBody AddPostContentReqDTO addPostContentReqDTO) {
        return postContentService.addPostContent(addPostContentReqDTO);
    }

    @PostMapping(value = "/post/content/find")
    public Response<FindPostContentRespDTO> findPostContent(@Validated @RequestBody FindPostContentReqDTO findPostContentReqDTO) {
        return postContentService.findPostContent(findPostContentReqDTO);
    }

    @PostMapping(value = "/post/content/delete")
    public Response<?> deletePostContent(@Validated @RequestBody DeletePostContentReqDTO deletePostContentReqDTO) {
        return postContentService.deletePostContent(deletePostContentReqDTO);
    }

}
