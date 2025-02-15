package work.licht.music.search.service;

import work.licht.music.common.response.PageResponse;
import work.licht.music.common.response.Response;
import work.licht.music.search.dto.RebuildUserSearchDocReqDTO;
import work.licht.music.search.model.vo.SearchUserReqVO;
import work.licht.music.search.model.vo.SearchUserRespVO;

// 用户搜索业务
public interface UserService {

    // 搜索用户
    PageResponse<SearchUserRespVO> searchUser(SearchUserReqVO searchUserReqVO);

    // 重建用户文档
    Response<Long> rebuildDocument(RebuildUserSearchDocReqDTO rebuildUserSearchDocReqDTO);

}
