package work.licht.music.post.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import work.licht.music.common.response.Response;
import work.licht.music.log.aspect.ApiOperationLog;
import work.licht.music.post.model.vo.*;
import work.licht.music.post.service.PostService;

@RestController
@RequestMapping("/post")
@Slf4j
public class PostController {

    @Resource
    private PostService postService;

    @PostMapping(value = "/publish")
    @ApiOperationLog(description = "帖子发布")
    public Response<?> publishPost(@Validated @RequestBody PublishPostReqVO publishPostReqVO) {
        return postService.publishPost(publishPostReqVO);
    }

    @PostMapping(value = "/detail")
    @ApiOperationLog(description = "帖子详情")
    public Response<FindPostDetailRespVO> findPostDetail(@Validated @RequestBody FindPostDetailReqVO findPostDetailReqVO) {
        return postService.findPostDetail(findPostDetailReqVO);
    }

    @PostMapping(value = "/update")
    @ApiOperationLog(description = "帖子修改")
    public Response<?> updatePost(@Validated @RequestBody UpdatePostReqVO updatePostReqVO) {
        return postService.updatePost(updatePostReqVO);
    }

    @PostMapping(value = "/delete")
    @ApiOperationLog(description = "帖子删除")
    public Response<?> deletePost(@Validated @RequestBody DeletePostReqVO deletePostReqVO) {
        return postService.deletePost(deletePostReqVO);
    }

    @PostMapping(value = "/visible")
    @ApiOperationLog(description = "修改帖子可见性")
    public Response<?> visibleOnlyMe(@Validated @RequestBody UpdatePostVisibleReqVO updatePostVisibleReqVO) {
        return postService.updatePostVisible(updatePostVisibleReqVO);
    }

    @PostMapping(value = "/top")
    @ApiOperationLog(description = "置顶/取消置顶帖子")
    public Response<?> topPost(@Validated @RequestBody UpdatePostIsTopReqVO updatePostIsTopReqVO) {
        return postService.updatePostIsTop(updatePostIsTopReqVO);
    }

    @PostMapping(value = "/like")
    @ApiOperationLog(description = "点赞帖子")
    public Response<?> likePost(@Validated @RequestBody LikePostReqVO likePostReqVO) {
        return postService.likePost(likePostReqVO);
    }

    @PostMapping(value = "/unlike")
    @ApiOperationLog(description = "取消点赞帖子")
    public Response<?> unlikePost(@Validated @RequestBody UnlikePostReqVO unlikePostReqVO) {
        return postService.unlikePost(unlikePostReqVO);
    }

    @PostMapping(value = "/collect")
    @ApiOperationLog(description = "收藏帖子")
    public Response<?> collectPost(@Validated @RequestBody CollectPostReqVO collectPostReqVO) {
        return postService.collectPost(collectPostReqVO);
    }

    @PostMapping(value = "/uncollect")
    @ApiOperationLog(description = "取消收藏帖子")
    public Response<?> unCollectPost(@Validated @RequestBody UnCollectPostReqVO unCollectPostReqVO) {
        return postService.unCollectPost(unCollectPostReqVO);
    }


}