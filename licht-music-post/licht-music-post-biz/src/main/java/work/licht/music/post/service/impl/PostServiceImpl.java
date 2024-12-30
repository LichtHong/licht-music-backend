package work.licht.music.post.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import work.licht.music.common.exception.BizException;
import work.licht.music.common.response.Response;
import work.licht.music.context.holder.LoginUserContextHolder;
import work.licht.music.post.constant.RedisKeyConstants;
import work.licht.music.post.constant.RocketMQConstants;
import work.licht.music.post.domain.mapper.PostDOMapper;
import work.licht.music.post.domain.mapper.TopicDOMapper;
import work.licht.music.post.domain.model.PostDO;
import work.licht.music.post.enums.PostStatusEnum;
import work.licht.music.post.enums.PostTypeEnum;
import work.licht.music.post.enums.PostVisibleEnum;
import work.licht.music.post.enums.ResponseCodeEnum;
import work.licht.music.post.model.vo.*;
import work.licht.music.post.rpc.IdRpcService;
import work.licht.music.post.rpc.KVRpcService;
import work.licht.music.post.rpc.UserRpcService;
import work.licht.music.post.service.PostService;
import work.licht.music.user.model.dto.resp.FindUserByIdRespDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PostServiceImpl implements PostService {
    // 帖子详情本地缓存
    private static final Cache<Long, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000) // 设置初始容量为 10000 个条目
            .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
            .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后 1 小时过期
            .build();

    @Resource
    private PostDOMapper postDOMapper;
    @Resource
    private TopicDOMapper topicDOMapper;
    @Resource
    private IdRpcService idRpcService;
    @Resource
    private KVRpcService kvRpcService;
    @Resource
    private UserRpcService userRpcService;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private Gson gson;

    // 帖子发布
    @Override
    public Response<?> publishPost(PublishPostReqVO publishPostReqVO) {
        // 帖子类型
        Integer type = publishPostReqVO.getType();
        // 获取对应类型的枚举
        PostTypeEnum postTypeEnum = PostTypeEnum.valueOf(type);
        // 若非图文、视频，抛出业务业务异常
        if (Objects.isNull(postTypeEnum)) throw new BizException(ResponseCodeEnum.POST_TYPE_ERROR);
        String imgUris = null;
        // 帖子内容是否为空，默认值为 true，即空
        boolean isContentEmpty = true;
        String videoUri = null;
        switch (postTypeEnum) {
            case IMAGE_TEXT: // 图文帖子
                List<String> imgUriList = publishPostReqVO.getImgUri();
                // 校验图片是否为空
                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "帖子图片不能为空");
                // 校验图片数量
                Preconditions.checkArgument(imgUriList.size() <= 8, "帖子图片不能多于 8 张");
                // 将图片链接拼接，以逗号分隔
                imgUris = StringUtils.join(imgUriList, ",");
                break;
            case VIDEO: // 视频帖子
                videoUri = publishPostReqVO.getVideoUri();
                // 校验视频链接是否为空
                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "帖子视频不能为空");
                break;
            default:
                break;
        }
        // RPC: 调用分布式 ID 生成服务，生成帖子 ID
        String snowflakeIdId = idRpcService.getPostId();
        // 帖子内容 UUID
        String contentUuid = null;
        // 帖子内容
        String content = publishPostReqVO.getContent();
        // 若用户填写了帖子内容
        if (StringUtils.isNotBlank(content)) {
            // 内容是否为空，置为 false，即不为空
            isContentEmpty = false;
            // 生成帖子内容 UUID
            contentUuid = UUID.randomUUID().toString();
            // RPC: 调用 KV 键值服务，存储短文本
            boolean isSavedSuccess = kvRpcService.savePostContent(contentUuid, content);
            // 若存储失败，抛出业务异常，提示用户发布帖子失败
            if (!isSavedSuccess) {
                throw new BizException(ResponseCodeEnum.POST_PUBLISH_FAIL);
            }
        }
        // 话题
        Long topicId = publishPostReqVO.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            // 获取话题名称
            topicName = topicDOMapper.selectNameByPrimaryKey(topicId);
        }
        // 发布者用户 ID
        Long creatorId = LoginUserContextHolder.getUserId();
        // 构建帖子 DO 对象
        PostDO postDO = PostDO.builder()
                .id(Long.valueOf(snowflakeIdId))
                .isContentEmpty(isContentEmpty)
                .creatorId(creatorId)
                .imgUri(imgUris)
                .title(publishPostReqVO.getTitle())
                .topicId(publishPostReqVO.getTopicId())
                .topicName(topicName)
                .type(type)
                .visible(PostVisibleEnum.PUBLIC.getCode())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .status(PostStatusEnum.NORMAL.getCode())
                .isTop(Boolean.FALSE)
                .videoUri(videoUri)
                .contentUuid(contentUuid)
                .build();
        try {
            // 帖子入库存储
            postDOMapper.insert(postDO);
        } catch (Exception e) {
            log.error("==> 帖子存储失败", e);
            // RPC: 帖子保存失败，则删除帖子内容
            if (StringUtils.isNotBlank(contentUuid)) {
                kvRpcService.deletePostContent(contentUuid);
            }
        }
        return Response.success();
    }

    // 帖子详情
    @Override
    @SneakyThrows
    public Response<FindPostDetailRespVO> findPostDetail(FindPostDetailReqVO findPostDetailReqVO) {
        // 查询的帖子 ID
        Long postId = findPostDetailReqVO.getId();
        // 当前登录用户
        Long userId = LoginUserContextHolder.getUserId();
        // 先从本地缓存中查询
        String findPostDetailRespVOStrLocalCache = LOCAL_CACHE.getIfPresent(postId);
        if (StringUtils.isNotBlank(findPostDetailRespVOStrLocalCache)) {
            FindPostDetailRespVO findPostDetailRespVO = gson.fromJson(findPostDetailRespVOStrLocalCache, FindPostDetailRespVO.class);
            log.info("==> 命中了本地缓存；{}", findPostDetailRespVOStrLocalCache);
            // 可见性校验
            checkPostVisibleFromVO(userId, findPostDetailRespVO);
            return Response.success(findPostDetailRespVO);
        }
        // 从 Redis 缓存中获取
        String postDetailKey = RedisKeyConstants.buildPostDetailKey(postId);
        String postDetailJson = redisTemplate.opsForValue().get(postDetailKey);
        // 若缓存中有该帖子的数据，则直接返回
        if (StringUtils.isNotBlank(postDetailJson)) {
            log.info("==> 命中了 Redis 缓存；{}", postDetailJson);
            FindPostDetailRespVO findPostDetailRespVO = gson.fromJson(postDetailJson, FindPostDetailRespVO.class);
            // 写入本地缓存
            threadPoolTaskExecutor.submit(() -> LOCAL_CACHE.put(postId, Objects.isNull(findPostDetailRespVO) ? "null" : postDetailJson));
            // 可见性校验
            checkPostVisibleFromVO(userId, findPostDetailRespVO);
            return Response.success(findPostDetailRespVO);
        }
        // 若 Redis 缓存中获取不到，则走数据库查询
        // 查询帖子
        PostDO postDO = postDOMapper.selectByPrimaryKey(postId);
        // 若该帖子不存在，则抛出业务异常
        if (Objects.isNull(postDO)) {
            threadPoolTaskExecutor.execute(() -> {
                // 防止缓存穿透，将空数据存入 Redis 缓存 (过期时间不宜设置过长)
                // 保底1分钟 + 随机秒数
                long expireSeconds = 60 + RandomUtil.randomInt(60);
                redisTemplate.opsForValue().set(postDetailKey, "null", expireSeconds, TimeUnit.SECONDS);
            });
            throw new BizException(ResponseCodeEnum.POST_NOT_FOUND);
        }
        // 可见性校验
        Integer visible = postDO.getVisible();
        checkPostVisible(visible, userId, postDO.getCreatorId());
        // 并发查询优化
        // RPC: 调用用户服务
        Long creatorId = postDO.getCreatorId();
        CompletableFuture<FindUserByIdRespDTO> userResultFuture = CompletableFuture.supplyAsync(() -> userRpcService.findById(creatorId), threadPoolTaskExecutor);
        // RPC: 调用 K-V 存储服务获取内容
        CompletableFuture<String> contentResultFuture = CompletableFuture.completedFuture(null);
        if (Objects.equals(postDO.getIsContentEmpty(), Boolean.FALSE)) {
            contentResultFuture = CompletableFuture.supplyAsync(() -> kvRpcService.findPostContent(postDO.getContentUuid()), threadPoolTaskExecutor);
        }
        CompletableFuture<String> finalContentResultFuture = contentResultFuture;
        CompletableFuture<FindPostDetailRespVO> resultFuture = CompletableFuture
                .allOf(userResultFuture, contentResultFuture)
                .thenApply(s -> {
                    // 获取 Future 返回的结果
                    FindUserByIdRespDTO findUserByIdRspDTO = userResultFuture.join();
                    String content = finalContentResultFuture.join();
                    // 帖子类型
                    Integer postType = postDO.getType();
                    // 图文帖子图片链接(字符串)
                    String imgUrisStr = postDO.getImgUri();
                    // 图文帖子图片链接(集合)
                    List<String> imgUri = null;
                    // 如果查询的是图文帖子，需要将图片链接的逗号分隔开，转换成集合
                    if (Objects.equals(postType, PostTypeEnum.IMAGE_TEXT.getCode()) && StringUtils.isNotBlank(imgUrisStr)) {
                        imgUri = List.of(imgUrisStr.split(","));
                    }
                    // 构建返参 VO 实体类
                    return FindPostDetailRespVO.builder()
                            .id(postDO.getId())
                            .type(postDO.getType())
                            .title(postDO.getTitle())
                            .content(content)
                            .imgUri(imgUri)
                            .topicId(postDO.getTopicId())
                            .topicName(postDO.getTopicName())
                            .creatorId(userId)
                            .creatorName(findUserByIdRspDTO.getNickName())
                            .avatar(findUserByIdRspDTO.getAvatar())
                            .videoUri(postDO.getVideoUri())
                            .updateTime(postDO.getUpdateTime())
                            .visible(postDO.getVisible())
                            .build();
                });
        // 获取拼装后的 FindPostDetailRespVO
        FindPostDetailRespVO findPostDetailRespVO = resultFuture.get();
        // 异步线程中将帖子详情存入 Redis
        threadPoolTaskExecutor.submit(() -> {
            String postDetailJson1 = gson.toJson(findPostDetailRespVO);
            // 过期时间（保底1天 + 随机秒数，将缓存过期时间打散，防止同一时间大量缓存失效，导致数据库压力太大）
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            redisTemplate.opsForValue().set(postDetailKey, postDetailJson1, expireSeconds, TimeUnit.SECONDS);
        });
        return Response.success(findPostDetailRespVO);
    }

    // 校验帖子的可见性（针对 VO 实体类）
    private void checkPostVisibleFromVO(Long userId, FindPostDetailRespVO findPostDetailRespVO) {
        if (Objects.nonNull(findPostDetailRespVO)) {
            Integer visible = findPostDetailRespVO.getVisible();
            checkPostVisible(visible, userId, findPostDetailRespVO.getCreatorId());
        }
    }

    // 校验帖子的可见性
    private void checkPostVisible(Integer visible, Long curUserId, Long creatorId) {
        if (Objects.equals(visible, PostVisibleEnum.PRIVATE.getCode()) && !Objects.equals(curUserId, creatorId)) {
            // 仅自己可见, 并且访问用户为帖子创建者
            throw new BizException(ResponseCodeEnum.POST_PRIVATE);
        }
    }

    // 帖子更新
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> updatePost(UpdatePostReqVO updatePostReqVO) {
        // 帖子 ID
        Long postId = updatePostReqVO.getId();
        // 帖子类型
        Integer type = updatePostReqVO.getType();
        // 获取对应类型的枚举
        PostTypeEnum postTypeEnum = PostTypeEnum.valueOf(type);
        // 若非图文、视频，抛出业务业务异常
        if (Objects.isNull(postTypeEnum)) throw new BizException(ResponseCodeEnum.POST_TYPE_ERROR);
        String imgUris = null;
        String videoUri = null;
        switch (postTypeEnum) {
            case IMAGE_TEXT: // 图文帖子
                List<String> imgUriList = updatePostReqVO.getImgUri();
                // 校验图片是否为空
                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "帖子图片不能为空");
                // 校验图片数量
                Preconditions.checkArgument(imgUriList.size() <= 8, "帖子图片不能多于 8 张");
                imgUris = StringUtils.join(imgUriList, ",");
                break;
            case VIDEO: // 视频帖子
                videoUri = updatePostReqVO.getVideoUri();
                // 校验视频链接是否为空
                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "帖子视频不能为空");
                break;
            default:
                break;
        }
        PostDO selectPostDO = postDOMapper.selectByPrimaryKey(postId);
        // 帖子不存在
        if (Objects.isNull(selectPostDO)) throw new BizException(ResponseCodeEnum.POST_NOT_FOUND);
        // 当前登录用户 ID
        Long currUserId = LoginUserContextHolder.getUserId();
        // 判断权限：非帖子发布者不允许更新帖子
        if (!Objects.equals(currUserId, selectPostDO.getCreatorId()))
            throw new BizException(ResponseCodeEnum.POST_CANT_OPERATE);
        // 话题
        Long topicId = updatePostReqVO.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            topicName = topicDOMapper.selectNameByPrimaryKey(topicId);
            // 判断一下提交的话题, 是否是真实存在的
            if (StringUtils.isBlank(topicName)) throw new BizException(ResponseCodeEnum.TOPIC_NOT_FOUND);
        }
        // 更新帖子元数据表 t_post
        String content = updatePostReqVO.getContent();
        PostDO postDO = PostDO.builder()
                .id(postId)
                .isContentEmpty(StringUtils.isBlank(content))
                .imgUri(imgUris)
                .title(updatePostReqVO.getTitle())
                .topicId(updatePostReqVO.getTopicId())
                .topicName(topicName)
                .type(type)
                .updateTime(LocalDateTime.now())
                .videoUri(videoUri)
                .build();
        postDOMapper.updateByPrimaryKey(postDO);
        // 删除 Redis 缓存
        String postDetailRedisKey = RedisKeyConstants.buildPostDetailKey(postId);
        redisTemplate.delete(postDetailRedisKey);
        // 删除本地缓存
        // LOCAL_CACHE.invalidate(postId);
        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rocketMQTemplate.syncSend(RocketMQConstants.TOPIC_DELETE_POST_LOCAL_CACHE, postId);
        log.info("====> MQ：删除帖子本地缓存发送成功... FROM：updatePost");
        // 帖子内容更新
        // 查询此篇帖子内容对应的 UUID
        PostDO postDO1 = postDOMapper.selectByPrimaryKey(postId);
        String contentUuid = postDO1.getContentUuid();
        // 帖子内容是否更新成功
        boolean isUpdateContentSuccess;
        if (StringUtils.isBlank(content)) {
            // 若帖子内容为空，则删除 K-V 存储
            isUpdateContentSuccess = kvRpcService.deletePostContent(contentUuid);
        } else {
            // 调用 K-V 更新短文本
            isUpdateContentSuccess = kvRpcService.savePostContent(contentUuid, content);
        }
        // 如果更新失败，抛出业务异常，回滚事务
        if (!isUpdateContentSuccess) {
            throw new BizException(ResponseCodeEnum.POST_UPDATE_FAIL);
        }
        return Response.success();
    }

    @Override
    public Response<?> updatePostVisible(UpdatePostVisibleReqVO updatePostVisibleReqVO) {
        Long postId = updatePostVisibleReqVO.getId();
        Integer visible = updatePostVisibleReqVO.getVisible();
        PostDO selectPostDO = postDOMapper.selectByPrimaryKey(postId);
        // 判断帖子是否存在
        if (Objects.isNull(selectPostDO)) throw new BizException(ResponseCodeEnum.POST_NOT_FOUND);
        // 判断权限：非帖子发布者不允许修改帖子权限
        Long currUserId = LoginUserContextHolder.getUserId();
        if (!Objects.equals(currUserId, selectPostDO.getCreatorId())) throw new BizException(ResponseCodeEnum.POST_CANT_OPERATE);
        // 构建更新 DO 实体类
        PostDO postDO = PostDO.builder()
                .id(postId)
                .visible(visible) // 可见性设置
                .creatorId(currUserId) // 只有帖子所有者，才能操作帖子
                .updateTime(LocalDateTime.now())
                .build();
        // 执行更新 SQL
        int count = postDOMapper.updateVisible(postDO);
        if (count == 0) throw new BizException(ResponseCodeEnum.POST_CANT_OPERATE);
        // 删除 Redis 缓存
        String postDetailRedisKey = RedisKeyConstants.buildPostDetailKey(postId);
        redisTemplate.delete(postDetailRedisKey);
        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rocketMQTemplate.syncSend(RocketMQConstants.TOPIC_DELETE_POST_LOCAL_CACHE, postId);
        log.info("====> MQ：删除帖子本地缓存发送成功... FROM：updatePostVisible");
        return Response.success();
    }

    @Override
    public Response<?> updatePostIsTop(UpdatePostIsTopReqVO updatePostIsTopReqVO) {
        Long postId = updatePostIsTopReqVO.getId();
        Boolean isTop = updatePostIsTopReqVO.getIsTop();
        Long currUserId = LoginUserContextHolder.getUserId();
        PostDO postDO = PostDO.builder()
                .id(postId)
                .isTop(isTop)
                .updateTime(LocalDateTime.now())
                .creatorId(currUserId) // 只有帖子所有者，才能置顶/取消置顶帖子
                .build();
        int count = postDOMapper.updateIsTop(postDO);
        if (count == 0) throw new BizException(ResponseCodeEnum.POST_CANT_OPERATE);
        // 删除 Redis 缓存
        String postDetailRedisKey = RedisKeyConstants.buildPostDetailKey(postId);
        redisTemplate.delete(postDetailRedisKey);
        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rocketMQTemplate.syncSend(RocketMQConstants.TOPIC_DELETE_POST_LOCAL_CACHE, postId);
        log.info("====> MQ：删除帖子本地缓存发送成功... FROM：updatePostIsTop");
        return Response.success();
    }

    // 删除帖子
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> deletePost(DeletePostReqVO deletePostReqVO) {
        // 帖子 ID
        Long postId = deletePostReqVO.getId();
        PostDO selectPostDO = postDOMapper.selectByPrimaryKey(postId);
        // 判断帖子是否存在
        if (Objects.isNull(selectPostDO)) throw new BizException(ResponseCodeEnum.POST_NOT_FOUND);
        // 判断权限：非帖子发布者不允许删除帖子
        Long currUserId = LoginUserContextHolder.getUserId();
        if (!Objects.equals(currUserId, selectPostDO.getCreatorId())) throw new BizException(ResponseCodeEnum.POST_CANT_OPERATE);
        // 逻辑删除
        PostDO postDO = PostDO.builder()
                .id(postId)
                .status(PostStatusEnum.DELETED.getCode())
                .updateTime(LocalDateTime.now())
                .build();
        int count = postDOMapper.updateByPrimaryKeySelective(postDO);
        // 若影响的行数为 0，则表示该帖子不存在
        if (count == 0) throw new BizException(ResponseCodeEnum.POST_NOT_FOUND);
        // 删除 Redis 缓存
        String postDetailRedisKey = RedisKeyConstants.buildPostDetailKey(postId);
        redisTemplate.delete(postDetailRedisKey);
        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rocketMQTemplate.syncSend(RocketMQConstants.TOPIC_DELETE_POST_LOCAL_CACHE, postId);
        log.info("====> MQ：删除帖子本地缓存发送成功...");
        return Response.success();
    }

    // 删除本地帖子缓存
    public void deletePostLocalCache(Long postId) {
        LOCAL_CACHE.invalidate(postId);
    }

}
