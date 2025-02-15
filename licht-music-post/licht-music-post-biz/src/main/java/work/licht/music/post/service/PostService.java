package work.licht.music.post.service;

import work.licht.music.common.response.Response;
import work.licht.music.post.model.vo.*;

public interface PostService {

    // 帖子发布
    Response<?> publishPost(PublishPostReqVO publishPostReqVO);

    // 帖子详情
    Response<FindPostDetailRespVO> findPostDetail(FindPostDetailReqVO findPostDetailReqVO);

    // 帖子更新
    Response<?> updatePost(UpdatePostReqVO updatePostReqVO);

    Response<?> updatePostVisible(UpdatePostVisibleReqVO updatePostVisibleReqVO);

    Response<?> updatePostIsTop(UpdatePostIsTopReqVO updatePostIsTopReqVO);

    // 删除帖子
    Response<?> deletePost(DeletePostReqVO deletePostReqVO);

    // 删除本地帖子缓存
    void deletePostLocalCache(Long postId);

    // 点赞帖子
    Response<?> likePost(LikePostReqVO likePostReqVO);

    // 取消点赞帖子
    Response<?> unlikePost(UnlikePostReqVO unlikePostReqVO);

    // 收藏帖子
    Response<?> collectPost(CollectPostReqVO collectPostReqVO);

    // 取消收藏帖子
    Response<?> unCollectPost(UnCollectPostReqVO unCollectPostReqVO);

}