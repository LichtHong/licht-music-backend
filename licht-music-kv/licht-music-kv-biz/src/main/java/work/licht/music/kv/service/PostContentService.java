package work.licht.music.kv.service;

import work.licht.music.common.response.Response;
import work.licht.music.kv.dto.req.AddPostContentReqDTO;
import work.licht.music.kv.dto.req.DeletePostContentReqDTO;
import work.licht.music.kv.dto.req.FindPostContentReqDTO;
import work.licht.music.kv.dto.resp.FindPostContentRespDTO;

public interface PostContentService {

    // 添加帖子内容
    Response<?> addPostContent(AddPostContentReqDTO addPostContentReqDTO);

    // 查询帖子内容
    Response<FindPostContentRespDTO> findPostContent(FindPostContentReqDTO findPostContentReqDTO);

    // 删除帖子内容
    Response<?> deletePostContent(DeletePostContentReqDTO deletePostContentReqDTO);

}