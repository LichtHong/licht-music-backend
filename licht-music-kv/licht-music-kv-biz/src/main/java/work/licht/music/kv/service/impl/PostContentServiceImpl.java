package work.licht.music.kv.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import work.licht.music.common.exception.BizException;
import work.licht.music.common.response.Response;
import work.licht.music.kv.domain.model.PostContentDO;
import work.licht.music.kv.domain.repository.PostContentRepository;
import work.licht.music.kv.model.dto.req.AddPostContentReqDTO;
import work.licht.music.kv.model.dto.req.DeletePostContentReqDTO;
import work.licht.music.kv.model.dto.req.FindPostContentReqDTO;
import work.licht.music.kv.model.dto.resp.FindPostContentRespDTO;
import work.licht.music.kv.enums.ResponseCodeEnum;
import work.licht.music.kv.service.PostContentService;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class PostContentServiceImpl implements PostContentService {

    @Resource
    private PostContentRepository postContentRepository;

    @Override
    public Response<?> addPostContent(AddPostContentReqDTO addPostContentReqDTO) {
        // 帖子 UUID
        String uuid = addPostContentReqDTO.getUuid();
        // 帖子内容
        String content = addPostContentReqDTO.getContent();
        // 构建数据库 DO 实体类
        PostContentDO nodeContent = PostContentDO.builder()
                .id(UUID.fromString(uuid))
                .content(content)
                .build();
        // 插入数据
        postContentRepository.save(nodeContent);
        return Response.success();
    }

    // 查询帖子内容
    @Override
    public Response<FindPostContentRespDTO> findPostContent(FindPostContentReqDTO findPostContentReqDTO) {
        // 帖子 ID
        String uuid = findPostContentReqDTO.getUuid();
        // 根据帖子ID查询帖子内容
        Optional<PostContentDO> optional = postContentRepository.findById(UUID.fromString(uuid));
        // 若帖子内容不存在
        if (optional.isEmpty()) {
            throw new BizException(ResponseCodeEnum.POST_CONTENT_NOT_FOUND);
        }
        PostContentDO postContentDO = optional.get();
        // 构建返参 DTO
        FindPostContentRespDTO findPostContentRespDTO = FindPostContentRespDTO.builder()
                .uuid(postContentDO.getId())
                .content(postContentDO.getContent())
                .build();
        return Response.success(findPostContentRespDTO);
    }

    // 删除帖子内容
    @Override
    public Response<?> deletePostContent(DeletePostContentReqDTO deletePostContentReqDTO) {
        // 帖子 ID
        String uuid = deletePostContentReqDTO.getUuid();
        // 删除帖子内容
        postContentRepository.deleteById(UUID.fromString(uuid));
        return Response.success();
    }

}