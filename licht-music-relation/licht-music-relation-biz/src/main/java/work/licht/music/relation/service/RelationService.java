package work.licht.music.relation.service;

import work.licht.music.common.response.PageResponse;
import work.licht.music.common.response.Response;
import work.licht.music.relation.model.vo.*;

public interface RelationService {

    // 关注用户
    Response<?> follow(FollowUserReqVO followUserReqVO);

    // 取关用户
    Response<?> unfollow(UnfollowUserReqVO unfollowUserReqVO);

    // 查询关注列表
    PageResponse<FindFollowerUserRespVO> findFollowerList(FindFollowerListReqVO findFollowerListReqVO);

    // 查询粉丝列表
    PageResponse<FindFanUserRespVO> findFanList(FindFanListReqVO findFanListReqVO);
}