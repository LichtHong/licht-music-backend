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
import work.licht.music.search.dto.RebuildUserSearchDocReqDTO;
import work.licht.music.search.model.vo.SearchUserReqVO;
import work.licht.music.search.model.vo.SearchUserRespVO;
import work.licht.music.search.service.UserService;


// 用户
@RestController
@RequestMapping("/search")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/user")
    @ApiOperationLog(description = "搜索用户")
    public PageResponse<SearchUserRespVO> searchUser(@RequestBody @Validated SearchUserReqVO searchUserReqVO) {
        return userService.searchUser(searchUserReqVO);
    }

    // ===================================== 对其他服务提供的接口 =====================================
    @PostMapping("/user/doc/rebuild")
    @ApiOperationLog(description = "用户文档重建")
    public Response<Long> rebuildDocument(@Validated @RequestBody RebuildUserSearchDocReqDTO rebuildUserSearchDocReqDTO) {
        return userService.rebuildDocument(rebuildUserSearchDocReqDTO);
    }

}