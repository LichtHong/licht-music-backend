package work.licht.music.relation.controller;

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
import work.licht.music.relation.model.vo.*;
import work.licht.music.relation.service.RelationService;

@RestController
@RequestMapping("/relation")
@Slf4j
public class RelationController {

    @Resource
    private RelationService relationService;

    @PostMapping("/follow")
    @ApiOperationLog(description = "关注用户")
    public Response<?> follow(@Validated @RequestBody FollowUserReqVO followUserReqVO) {
        return relationService.follow(followUserReqVO);
    }

    @PostMapping("/unfollow")
    @ApiOperationLog(description = "取关用户")
    public Response<?> unfollow(@Validated @RequestBody UnfollowUserReqVO unfollowUserReqVO) {
        return relationService.unfollow(unfollowUserReqVO);
    }

    @PostMapping("/follower/list")
    @ApiOperationLog(description = "查询用户关注列表")
    public PageResponse<FindFollowerUserRespVO> findFollowerList(@Validated @RequestBody FindFollowerListReqVO findFollowerListReqVO) {
        return relationService.findFollowerList(findFollowerListReqVO);
    }

    @PostMapping("/fan/list")
    @ApiOperationLog(description = "查询用户粉丝列表")
    public PageResponse<FindFanUserRespVO> findFansList(@Validated @RequestBody FindFanListReqVO findFanListReqVO) {
        return relationService.findFanList(findFanListReqVO);
    }

}