package work.licht.music.search.service;

import work.licht.music.common.response.PageResponse;
import work.licht.music.common.response.Response;
import work.licht.music.search.dto.RebuildPostSearchDocReqDTO;
import work.licht.music.search.model.vo.SearchPostReqVO;
import work.licht.music.search.model.vo.SearchPostRespVO;

// 帖子搜索业务
public interface PostService {

    // 搜索帖子
    PageResponse<SearchPostRespVO> searchPost(SearchPostReqVO searchPostReqVO);

    // 重建帖子文档
    Response<Long> rebuildSearchDocument(RebuildPostSearchDocReqDTO rebuildPostSearchDocReqDTO);

}
