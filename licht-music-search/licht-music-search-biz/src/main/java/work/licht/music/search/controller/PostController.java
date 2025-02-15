package work.licht.music.search.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import work.licht.music.common.response.PageResponse;
import work.licht.music.common.response.Response;
import work.licht.music.log.aspect.ApiOperationLog;
import work.licht.music.search.dto.RebuildPostSearchDocReqDTO;
import work.licht.music.search.model.vo.SearchPostReqVO;
import work.licht.music.search.model.vo.SearchPostRespVO;
import work.licht.music.search.service.PostService;

// 帖子搜索
@RestController
@RequestMapping("/search")
@Slf4j
public class PostController {

    @Resource
    private PostService postService;

    @PostMapping("/post")
    @ApiOperationLog(description = "搜索帖子")
    public PageResponse<SearchPostRespVO> searchPost(@RequestBody @Validated SearchPostReqVO searchPostReqVO) {
        return postService.searchPost(searchPostReqVO);
    }

    // ===================================== 对其他服务提供的接口 =====================================
    @PostMapping("/post/doc/rebuild")
    @ApiOperationLog(description = "帖子搜索文档重建")
    public Response<Long> rebuildSearchDoc(@Validated @RequestBody RebuildPostSearchDocReqDTO rebuildPostSearchDocReqDTO) {
        return postService.rebuildSearchDocument(rebuildPostSearchDocReqDTO);
    }

}