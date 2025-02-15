package work.licht.music.post.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import work.licht.music.common.exception.BizException;
import work.licht.music.common.response.Response;
import work.licht.music.common.util.DateUtils;
import work.licht.music.context.holder.LoginUserContextHolder;
import work.licht.music.post.constant.RedisKeyConstants;
import work.licht.music.post.constant.RocketMQConstants;
import work.licht.music.post.domain.mapper.PostCollectDOMapper;
import work.licht.music.post.domain.mapper.PostDOMapper;
import work.licht.music.post.domain.mapper.PostLikeDOMapper;
import work.licht.music.post.domain.mapper.TopicDOMapper;
import work.licht.music.post.domain.model.PostCollectDO;
import work.licht.music.post.domain.model.PostDO;
import work.licht.music.post.domain.model.PostLikeDO;
import work.licht.music.post.enums.*;
import work.licht.music.post.model.dto.CollectUnCollectPostMqDTO;
import work.licht.music.post.model.dto.LikeUnlikePostMqDTO;
import work.licht.music.post.model.dto.PostOperateMqDTO;
import work.licht.music.post.model.vo.*;
import work.licht.music.post.rpc.IdRpcService;
import work.licht.music.post.rpc.KVRpcService;
import work.licht.music.post.rpc.UserRpcService;
import work.licht.music.post.service.PostService;
import work.licht.music.user.model.dto.resp.FindUserByIdRespDTO;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PostServiceImpl implements PostService {
    // 帖子详情本地缓存
    private static final Cache<Long, String> LOCAL_CACHE = Caffeine.newBuilder().initialCapacity(10000) // 设置初始容量为 10000 个条目
            .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
            .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后 1 小时过期
            .build();

    @Resource
    private PostDOMapper postDOMapper;
    @Resource
    private TopicDOMapper topicDOMapper;
    @Resource
    private PostLikeDOMapper postLikeDOMapper;
    @Resource
    private PostCollectDOMapper postCollectDOMapper;
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

    // 构建 Lua 脚本参数
    private static Object[] buildPostLikeZSetLuaArgs(List<PostLikeDO> postLikeDOList, long expireSeconds) {
        // 每个帖子点赞关系有 2 个参数（score 和 value），最后再跟一个过期时间
        int argsLength = postLikeDOList.size() * 2 + 1;
        Object[] luaArgs = new Object[argsLength];
        int i = 0;
        for (PostLikeDO postLikeDO : postLikeDOList) {
            // 点赞时间作为 score
            luaArgs[i] = DateUtils.localDateTime2Timestamp(postLikeDO.getCreateTime());
            // 帖子ID作为 ZSet value
            luaArgs[i + 1] = postLikeDO.getPostId();
            i += 2;
        }
        // 最后一个参数是 ZSet 的过期时间
        luaArgs[argsLength - 1] = expireSeconds;
        return luaArgs;
    }

    // 构建帖子收藏 ZSET Lua 脚本参数
    private static Object[] buildPostCollectZSetLuaArgs(List<PostCollectDO> postCollectDOList, long expireSeconds) {
        int argsLength = postCollectDOList.size() * 2 + 1; // 每个帖子收藏关系有 2 个参数（score 和 value），最后再跟一个过期时间
        Object[] luaArgs = new Object[argsLength];
        int i = 0;
        for (PostCollectDO postCollectDO : postCollectDOList) {
            // 收藏时间作为 score
            luaArgs[i] = DateUtils.localDateTime2Timestamp(postCollectDO.getCreateTime());
            // 帖子ID作为 ZSet value
            luaArgs[i + 1] = postCollectDO.getPostId();
            i += 2;
        }
        // 最后一个参数是 ZSet 的过期时间
        luaArgs[argsLength - 1] = expireSeconds;
        return luaArgs;
    }

    // 帖子发布
    @Override
    public Response<?> publishPost(PublishPostReqVO publishPostReqVO) {
        // RPC: 调用分布式 ID 生成服务，生成帖子 ID
        String snowflakeIdId = idRpcService.getPostId();
        // 帖子内容 UUID
        String contentUuid = null;
        // 帖子内容
        String content = publishPostReqVO.getContent();
        // 若用户填写了帖子内容
        if (StringUtils.isNotBlank(content)) {
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
                .creatorId(creatorId)
                .cover(publishPostReqVO.getCover())
                .title(publishPostReqVO.getTitle())
                .topicId(publishPostReqVO.getTopicId()).topicName(topicName)
                .visible(PostVisibleEnum.PUBLIC.getCode())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .status(PostStatusEnum.NORMAL.getCode())
                .isTop(Boolean.FALSE)
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
        // 发送 MQ
        // 构建消息体 DTO
        PostOperateMqDTO postOperateMqDTO = PostOperateMqDTO.builder()
                .creatorId(creatorId)
                .postId(Long.valueOf(snowflakeIdId))
                .type(PostOperateEnum.PUBLISH.getCode()) // 发布帖子
                .build();
        // 构建消息对象，并将 DTO 转成 Json 字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(gson.toJson(postOperateMqDTO)).build();
        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = RocketMQConstants.TOPIC_POST_OPERATE + ":" + RocketMQConstants.TAG_POST_PUBLISH;
        // 异步发送 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【帖子发布】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【帖子发布】MQ 发送异常: ", throwable);
            }
        });
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
        String contentUuid = postDO.getContentUuid();
        if (StringUtils.isNotBlank(contentUuid)) {
            contentResultFuture = CompletableFuture.supplyAsync(() -> kvRpcService.findPostContent(contentUuid), threadPoolTaskExecutor);
        }
        CompletableFuture<String> finalContentResultFuture = contentResultFuture;
        CompletableFuture<FindPostDetailRespVO> resultFuture = CompletableFuture.allOf(userResultFuture, contentResultFuture).thenApply(s -> {
            // 获取 Future 返回的结果
            FindUserByIdRespDTO findUserByIdRspDTO = userResultFuture.join();
            String content = finalContentResultFuture.join();
            // 构建返参 VO 实体类
            return FindPostDetailRespVO.builder()
                    .id(postDO.getId())
                    .title(postDO.getTitle())
                    .content(content)
                    .cover(postDO.getCover())
                    .topicId(postDO.getTopicId())
                    .topicName(postDO.getTopicName())
                    .creatorId(userId)
                    .creatorName(findUserByIdRspDTO.getNickName())
                    .avatar(findUserByIdRspDTO.getAvatar())
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
                .title(updatePostReqVO.getTitle())
                .cover(updatePostReqVO.getCover())
                .topicId(updatePostReqVO.getTopicId())
                .topicName(topicName)
                .updateTime(LocalDateTime.now())
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
        if (!Objects.equals(currUserId, selectPostDO.getCreatorId()))
            throw new BizException(ResponseCodeEnum.POST_CANT_OPERATE);
        // 构建更新 DO 实体类
        PostDO postDO = PostDO.builder().id(postId).visible(visible) // 可见性设置
                .creatorId(currUserId) // 只有帖子所有者，才能操作帖子
                .updateTime(LocalDateTime.now()).build();
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
        PostDO postDO = PostDO.builder().id(postId).isTop(isTop).updateTime(LocalDateTime.now()).creatorId(currUserId) // 只有帖子所有者，才能置顶/取消置顶帖子
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
        if (!Objects.equals(currUserId, selectPostDO.getCreatorId()))
            throw new BizException(ResponseCodeEnum.POST_CANT_OPERATE);
        // 逻辑删除
        PostDO postDO = PostDO.builder().id(postId).status(PostStatusEnum.DELETED.getCode()).updateTime(LocalDateTime.now()).build();
        int count = postDOMapper.updateByPrimaryKeySelective(postDO);
        // 若影响的行数为 0，则表示该帖子不存在
        if (count == 0) throw new BizException(ResponseCodeEnum.POST_NOT_FOUND);
        // 删除 Redis 缓存
        String postDetailRedisKey = RedisKeyConstants.buildPostDetailKey(postId);
        redisTemplate.delete(postDetailRedisKey);
        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rocketMQTemplate.syncSend(RocketMQConstants.TOPIC_DELETE_POST_LOCAL_CACHE, postId);
        log.info("====> MQ：删除帖子本地缓存发送成功...");
        // 发送 MQ
        // 构建消息体 DTO
        PostOperateMqDTO postOperateMqDTO = PostOperateMqDTO.builder()
                .creatorId(selectPostDO.getCreatorId())
                .postId(postId)
                .type(PostOperateEnum.DELETE.getCode()) // 删除帖子
                .build();
        // 构建消息对象，并将 DTO 转成 Json 字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(gson.toJson(postOperateMqDTO)).build();
        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = RocketMQConstants.TOPIC_POST_OPERATE + ":" + RocketMQConstants.TAG_POST_DELETE;
        // 异步发送 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【帖子删除】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【帖子删除】MQ 发送异常: ", throwable);
            }
        });
        return Response.success();
    }

    // 删除本地帖子缓存
    public void deletePostLocalCache(Long postId) {
        LOCAL_CACHE.invalidate(postId);
    }

    // 点赞帖子
    @Override
    public Response<?> likePost(LikePostReqVO likePostReqVO) {
        Long postId = likePostReqVO.getId();
        // 1. 校验被点赞的帖子是否存在，若存在，则获取发布者用户 ID
        Long creatorId = checkPostIsExistAndGetCreatorId(postId);
        // 2.判断目标帖子，是否已经点赞过
        // 当前登录用户ID
        Long userId = LoginUserContextHolder.getUserId();
        // 布隆过滤器Key
        String bloomUserPostLikeListKey = RedisKeyConstants.buildBloomUserPostLikeListKey(userId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_post_like_check.lua")));
        // 返回值类型
        script.setResultType(Long.class);
        // 执行Lua脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserPostLikeListKey), postId);
        // 用户点赞列表ZSet Key
        String userPostLikeZSetKey = RedisKeyConstants.buildUserPostLikeZSetKey(userId);
        PostLikeLuaResultEnum postLikeLuaResultEnum = PostLikeLuaResultEnum.valueOf(result);
        switch (Objects.requireNonNull(postLikeLuaResultEnum)) {
            // Redis中布隆过滤器不存在
            case NOT_EXIST -> {
                // 从数据库中校验帖子是否被点赞，并异步初始化布隆过滤器，设置过期时间
                int count = postLikeDOMapper.selectCountByUserIdAndPostId(userId, postId);
                // 保底1天+随机秒数
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                // 目标帖子已经被点赞
                if (count > 0) {
                    // 异步初始化布隆过滤器
                    threadPoolTaskExecutor.submit(() -> batchAddPostLike2BloomAndExpire(userId, expireSeconds, bloomUserPostLikeListKey));
                    throw new BizException(ResponseCodeEnum.POST_ALREADY_LIKED);
                }
                // 若目标帖子未被点赞，查询当前用户是否有点赞其他帖子，有则同步初始化布隆过滤器
                batchAddPostLike2BloomAndExpire(userId, expireSeconds, bloomUserPostLikeListKey);
                // 添加当前点赞帖子ID到布隆过滤器中
                // Lua脚本路径
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_add_post_like_and_expire.lua")));
                // 返回值类型
                script.setResultType(Long.class);
                redisTemplate.execute(script, Collections.singletonList(bloomUserPostLikeListKey), postId, expireSeconds);
            }
            // 目标帖子已经被点赞
            case POST_LIKED -> {
                Double score = redisTemplate.opsForZSet().score(userPostLikeZSetKey, postId);
                if (Objects.nonNull(score)) throw new BizException(ResponseCodeEnum.POST_ALREADY_LIKED);
                // 若 Score 为空，则表示 ZSet 点赞列表中不存在，查询数据库校验
                int count = postLikeDOMapper.selectPostIsLiked(userId, postId);
                if (count > 0) {
                    asyncInitUserPostLikesZSet(userId, userPostLikeZSetKey);
                    throw new BizException(ResponseCodeEnum.POST_ALREADY_LIKED);
                }
            }
        }
        // 3.更新用户ZSET点赞列表
        // Lua脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/post_like_check_and_update_zset.lua")));
        // 返回值类型
        script.setResultType(Long.class);
        // 当前时间
        LocalDateTime now = LocalDateTime.now();
        // 执行Lua脚本，拿到返回结果
        result = redisTemplate.execute(script, Collections.singletonList(userPostLikeZSetKey), postId, DateUtils.localDateTime2Timestamp(now));
        // 若ZSet列表不存在，需要重新初始化
        if (Objects.equals(result, PostLikeLuaResultEnum.NOT_EXIST.getCode())) {
            // 查询当前用户最新点赞的100篇帖子
            List<PostLikeDO> postLikeDOList = postLikeDOMapper.selectLikedByUserIdAndLimit(userId, 100);
            if (CollUtil.isNotEmpty(postLikeDOList)) {
                // 保底1天+随机秒数
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                // 构建Lua参数
                Object[] luaArgs = buildPostLikeZSetLuaArgs(postLikeDOList, expireSeconds);
                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                // Lua脚本路径
                script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_post_like_zset_and_expire.lua")));
                // 返回值类型
                script2.setResultType(Long.class);
                redisTemplate.execute(script2, Collections.singletonList(userPostLikeZSetKey), luaArgs);
                // 再次调用 post_like_check_and_update_zset.lua 脚本，将点赞的帖子添加到 zset 中
                redisTemplate.execute(script, Collections.singletonList(userPostLikeZSetKey), postId, DateUtils.localDateTime2Timestamp(now));
            }
        }
        // 4.发送MQ，将点赞数据落库
        // 构建消息体DTO
        LikeUnlikePostMqDTO likeUnlikePostMqDTO = LikeUnlikePostMqDTO.builder()
                .userId(userId)
                .postId(postId)
                .postCreatorId(creatorId)
                .type(LikeUnlikePostTypeEnum.LIKE.getCode()) // 点赞帖子
                .createTime(now)
                .build();
        // 构建消息对象，并将DTO转成Json字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(gson.toJson(likeUnlikePostMqDTO)).build();
        // 通过冒号连接, 可让MQ发送给主题Topic时，携带上标签Tag
        String destination = RocketMQConstants.TOPIC_LIKE_OR_UNLIKE + ":" + RocketMQConstants.TAG_LIKE;
        String hashKey = String.valueOf(userId);
        // 异步发送MQ消息，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【帖子点赞】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【帖子点赞】MQ 发送异常: ", throwable);
            }
        });
        return Response.success();
    }

    // 校验帖子是否存在，若存在，则获取帖子的发布者 ID
    private Long checkPostIsExistAndGetCreatorId(Long postId) {
        // 先从本地缓存校验
        String findPostDetailRspVOStrLocalCache = LOCAL_CACHE.getIfPresent(postId);
        // 解析 Json 字符串为 VO 对象
        FindPostDetailRespVO findPostDetailRespVO = gson.fromJson(findPostDetailRspVOStrLocalCache, FindPostDetailRespVO.class);
        // 若本地缓存没有
        if (Objects.isNull(findPostDetailRespVO)) {
            // 再从 Redis 中校验
            String postDetailRedisKey = RedisKeyConstants.buildPostDetailKey(postId);
            String postDetailJson = redisTemplate.opsForValue().get(postDetailRedisKey);
            // 解析 Json 字符串为 VO 对象
            findPostDetailRespVO = gson.fromJson(postDetailJson, FindPostDetailRespVO.class);
            // 都不存在，再查询数据库校验是否存在
            if (Objects.isNull(findPostDetailRespVO)) {
                // 帖子发布者用户 ID
                Long creatorId = postDOMapper.selectCreatorIdByPostId(postId);
                // 若数据库中也不存在，提示用户
                if (Objects.isNull(creatorId)) {
                    throw new BizException(ResponseCodeEnum.POST_NOT_FOUND);
                }
                // 若数据库中存在，异步同步一下缓存
                threadPoolTaskExecutor.submit(() -> {
                    FindPostDetailReqVO findPostDetailReqVO = FindPostDetailReqVO.builder().id(postId).build();
                    findPostDetail(findPostDetailReqVO);
                });
                return creatorId;
            }
        }
        return findPostDetailRespVO.getCreatorId();
    }


    // 异步初始化布隆过滤器
    private void batchAddPostLike2BloomAndExpire(Long userId, long expireSeconds, String bloomUserPostLikeListKey) {
        try {
            // 异步全量同步一下，并设置过期时间
            List<PostLikeDO> postLikeDOList = postLikeDOMapper.selectByUserId(userId);
            if (CollUtil.isNotEmpty(postLikeDOList)) {
                DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                // Lua脚本路径
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_post_like_and_expire.lua")));
                // 返回值类型
                script.setResultType(Long.class);
                // 构建Lua参数
                List<Object> luaArgs = Lists.newArrayList();
                // 将每个点赞的帖子ID传入
                postLikeDOList.forEach(postLikeDO -> luaArgs.add(postLikeDO.getPostId()));
                // 最后一个参数是过期时间（秒）
                luaArgs.add(expireSeconds);
                redisTemplate.execute(script, Collections.singletonList(bloomUserPostLikeListKey), luaArgs.toArray());
            }
        } catch (Exception e) {
            log.error("## 初始化布隆过滤器异常: ", e);
        }
    }

    // 异步初始化用户点赞帖子ZSet
    private void asyncInitUserPostLikesZSet(Long userId, String userPostLikeZSetKey) {
        threadPoolTaskExecutor.execute(() -> {
            // 判断用户帖子点赞ZSET是否存在
            boolean hasKey = redisTemplate.hasKey(userPostLikeZSetKey);
            // 不存在，则重新初始化
            if (!hasKey) {
                // 查询当前用户最新点赞的100篇帖子
                List<PostLikeDO> postLikeDOList = postLikeDOMapper.selectLikedByUserIdAndLimit(userId, 100);
                if (CollUtil.isNotEmpty(postLikeDOList)) {
                    // 保底1天+随机秒数
                    long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                    // 构建 Lua 参数
                    Object[] luaArgs = buildPostLikeZSetLuaArgs(postLikeDOList, expireSeconds);
                    DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                    // Lua 脚本路径
                    script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_post_like_zset_and_expire.lua")));
                    // 返回值类型
                    script2.setResultType(Long.class);
                    redisTemplate.execute(script2, Collections.singletonList(userPostLikeZSetKey), luaArgs);
                }
            }
        });
    }

    // 取消点赞帖子
    @Override
    public Response<?> unlikePost(UnlikePostReqVO unlikePostReqVO) {
        // 帖子ID
        Long postId = unlikePostReqVO.getId();
        // 1. 校验被点赞的帖子是否存在，若存在，则获取发布者用户 ID
        Long creatorId = checkPostIsExistAndGetCreatorId(postId);
        // 2. 校验帖子是否被点赞过
        // 当前登录用户ID
        Long userId = LoginUserContextHolder.getUserId();
        // 布隆过滤器 Key
        String bloomUserPostLikeListKey = RedisKeyConstants.buildBloomUserPostLikeListKey(userId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_post_unlike_check.lua")));
        // 返回值类型
        script.setResultType(Long.class);
        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserPostLikeListKey), postId);
        PostUnlikeLuaResultEnum postUnlikeLuaResultEnum = PostUnlikeLuaResultEnum.valueOf(result);
        switch (Objects.requireNonNull(postUnlikeLuaResultEnum)) {
            // 布隆过滤器不存在
            case NOT_EXIST -> {
                // 异步初始化布隆过滤器
                threadPoolTaskExecutor.submit(() -> {
                    // 保底1天+随机秒数
                    long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                    batchAddPostLike2BloomAndExpire(userId, expireSeconds, bloomUserPostLikeListKey);
                });
                // 从数据库中校验帖子是否被点赞
                int count = postLikeDOMapper.selectCountByUserIdAndPostId(userId, postId);
                // 未点赞，无法取消点赞操作，抛出业务异常
                if (count == 0) throw new BizException(ResponseCodeEnum.POST_NOT_LIKED);
            }
            // 布隆过滤器校验目标帖子未被点赞（判断绝对正确）
            case POST_NOT_LIKED -> throw new BizException(ResponseCodeEnum.POST_NOT_LIKED);
        }
        // 3. 布隆过滤器判断已点赞，直接删除ZSET中已点赞的帖子ID
        // 用户点赞列表 ZSet Key
        String userPostLikeZSetKey = RedisKeyConstants.buildUserPostLikeZSetKey(userId);
        redisTemplate.opsForZSet().remove(userPostLikeZSetKey, postId);
        // 4. 发送 MQ，数据更新落库
        // 构建消息体 DTO
        LikeUnlikePostMqDTO likeUnlikePostMqDTO = LikeUnlikePostMqDTO.builder()
                .userId(userId)
                .postId(postId)
                .postCreatorId(creatorId)
                .type(LikeUnlikePostTypeEnum.UNLIKE.getCode()) // 取消点赞帖子
                .createTime(LocalDateTime.now())
                .build();
        // 构建消息对象，并将DTO转成Json字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(gson.toJson(likeUnlikePostMqDTO)).build();
        // 通过冒号连接, 让MQ发送给主题Topic时，携带上标签Tag
        String destination = RocketMQConstants.TOPIC_LIKE_OR_UNLIKE + ":" + RocketMQConstants.TAG_UNLIKE;
        String hashKey = String.valueOf(userId);
        // 异步发送 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【帖子取消点赞】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【帖子取消点赞】MQ 发送异常: ", throwable);
            }
        });
        return Response.success();
    }

    // 收藏帖子
    @Override
    public Response<?> collectPost(CollectPostReqVO collectPostReqVO) {
        // 帖子ID
        Long postId = collectPostReqVO.getId();
        // 1. 校验被收藏的帖子是否存在，若存在，则获取发布者用户 ID
        Long creatorId = checkPostIsExistAndGetCreatorId(postId);
        // 2.判断目标帖子，是否已经收藏过
        // 当前登录用户ID
        Long userId = LoginUserContextHolder.getUserId();
        // 布隆过滤器 Key
        String bloomUserPostCollectListKey = RedisKeyConstants.buildBloomUserPostCollectListKey(userId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_post_collect_check.lua")));
        // 返回值类型
        script.setResultType(Long.class);
        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserPostCollectListKey), postId);
        // 用户收藏列表 ZSet Key
        String userPostCollectZSetKey = RedisKeyConstants.buildUserPostCollectZSetKey(userId);
        PostCollectLuaResultEnum postCollectLuaResultEnum = PostCollectLuaResultEnum.valueOf(result);
        switch (Objects.requireNonNull(postCollectLuaResultEnum)) {
            // Redis 中布隆过滤器不存在
            case NOT_EXIST -> {
                // 从数据库中校验帖子是否被收藏，并异步初始化布隆过滤器，设置过期时间
                int count = postCollectDOMapper.selectCountByUserIdAndPostId(userId, postId);
                // 保底1天+随机秒数
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                // 目标帖子已经被收藏
                if (count > 0) {
                    // 异步初始化布隆过滤器
                    threadPoolTaskExecutor.submit(() -> batchAddPostCollect2BloomAndExpire(userId, expireSeconds, bloomUserPostCollectListKey));
                    throw new BizException(ResponseCodeEnum.POST_ALREADY_COLLECTED);
                }
                // 同步初始化布隆过滤器
                batchAddPostCollect2BloomAndExpire(userId, expireSeconds, bloomUserPostCollectListKey);
                // 添加当前收藏帖子ID到布隆过滤器中
                // Lua 脚本路径
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_add_post_collect_and_expire.lua")));
                // 返回值类型
                script.setResultType(Long.class);
                redisTemplate.execute(script, Collections.singletonList(bloomUserPostCollectListKey), postId, expireSeconds);
            }
            // 目标帖子已经被收藏，可能存在误判，需要进一步确认
            case POST_COLLECTED -> {
                // 校验 ZSet 列表中是否包含被收藏的帖子ID
                Double score = redisTemplate.opsForZSet().score(userPostCollectZSetKey, postId);
                if (Objects.nonNull(score)) {
                    throw new BizException(ResponseCodeEnum.POST_ALREADY_COLLECTED);
                }
                // 若 Score 为空，则表示 ZSet 收藏列表中不存在，查询数据库校验
                int count = postCollectDOMapper.selectCountByUserIdAndPostId(userId, postId);
                if (count > 0) {
                    // 数据库里面有收藏记录，而 Redis 中 ZSet 已过期被删除的话，需要重新异步初始化 ZSet
                    asyncInitUserPostCollectsZSet(userId, userPostCollectZSetKey);
                    throw new BizException(ResponseCodeEnum.POST_ALREADY_COLLECTED);
                }
            }
        }
        // 3. 更新用户 ZSET 收藏列表
        LocalDateTime now = LocalDateTime.now();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/post_collect_check_and_update_zset.lua")));
        // 返回值类型
        script.setResultType(Long.class);
        // 执行 Lua 脚本，拿到返回结果
        result = redisTemplate.execute(script, Collections.singletonList(userPostCollectZSetKey), postId, DateUtils.localDateTime2Timestamp(now));
        // 若 ZSet 列表不存在，需要重新初始化
        if (Objects.equals(result, PostCollectLuaResultEnum.NOT_EXIST.getCode())) {
            // 查询当前用户最新收藏的 300 篇帖子
            List<PostCollectDO> postCollectDOList = postCollectDOMapper.selectCollectedByUserIdAndLimit(userId, 300);
            if (CollUtil.isNotEmpty(postCollectDOList)) {
                // 保底1天+随机秒数
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                // 构建 Lua 参数
                Object[] luaArgs = buildPostCollectZSetLuaArgs(postCollectDOList, expireSeconds);
                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                // Lua 脚本路径
                script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_post_collect_zset_and_expire.lua")));
                // 返回值类型
                script2.setResultType(Long.class);
                redisTemplate.execute(script2, Collections.singletonList(userPostCollectZSetKey), luaArgs);
                // 再次调用 post_collect_check_and_update_zset.lua 脚本，将当前收藏的帖子添加到 zset 中
                redisTemplate.execute(script, Collections.singletonList(userPostCollectZSetKey), postId, DateUtils.localDateTime2Timestamp(now));
            }
        }
        // 4. 发送 MQ, 将收藏数据落库
        // 构建消息体 DTO
        CollectUnCollectPostMqDTO collectUnCollectPostMqDTO = CollectUnCollectPostMqDTO.builder()
                .userId(userId)
                .postId(postId)
                .postCreatorId(creatorId)
                .type(CollectUnCollectPostTypeEnum.COLLECT.getCode()) // 收藏帖子
                .createTime(now)
                .build();
        // 构建消息对象，并将 DTO 转成 Json 字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(gson.toJson(collectUnCollectPostMqDTO)).build();
        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = RocketMQConstants.TOPIC_COLLECT_OR_UN_COLLECT + ":" + RocketMQConstants.TAG_COLLECT;
        String hashKey = String.valueOf(userId);
        // 异步发送顺序 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【帖子收藏】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【帖子收藏】MQ 发送异常: ", throwable);
            }
        });
        return Response.success();
    }

    // 初始化帖子收藏布隆过滤器
    private void batchAddPostCollect2BloomAndExpire(Long userId, long expireSeconds, String bloomUserPostCollectListKey) {
        try {
            // 异步全量同步一下，并设置过期时间
            List<PostCollectDO> postCollectDOList = postCollectDOMapper.selectByUserId(userId);
            if (CollUtil.isNotEmpty(postCollectDOList)) {
                DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                // Lua 脚本路径
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_post_collect_and_expire.lua")));
                // 返回值类型
                script.setResultType(Long.class);
                // 构建 Lua 参数
                List<Object> luaArgs = Lists.newArrayList();
                postCollectDOList.forEach(postCollectDO -> luaArgs.add(postCollectDO.getPostId())); // 将每个收藏的帖子 ID 传入
                luaArgs.add(expireSeconds);  // 最后一个参数是过期时间（秒）
                redisTemplate.execute(script, Collections.singletonList(bloomUserPostCollectListKey), luaArgs.toArray());
            }
        } catch (Exception e) {
            log.error("## 初始化【帖子收藏】布隆过滤器异常: ", e);
        }
    }

    // 异步初始化用户收藏帖子ZSet
    private void asyncInitUserPostCollectsZSet(Long userId, String userPostCollectZSetKey) {
        threadPoolTaskExecutor.execute(() -> {
            // 判断用户帖子收藏 ZSET 是否存在
            boolean hasKey = redisTemplate.hasKey(userPostCollectZSetKey);
            // 不存在，则重新初始化
            if (!hasKey) {
                // 查询当前用户最新收藏的 300 篇帖子
                List<PostCollectDO> postCollectDOList = postCollectDOMapper.selectCollectedByUserIdAndLimit(userId, 300);
                if (CollUtil.isNotEmpty(postCollectDOList)) {
                    // 保底1天+随机秒数
                    long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                    // 构建 Lua 参数
                    Object[] luaArgs = buildPostCollectZSetLuaArgs(postCollectDOList, expireSeconds);
                    DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                    // Lua 脚本路径
                    script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/batch_add_post_collect_zset_and_expire.lua")));
                    // 返回值类型
                    script2.setResultType(Long.class);
                    redisTemplate.execute(script2, Collections.singletonList(userPostCollectZSetKey), luaArgs);
                }
            }
        });
    }

    // 取消收藏帖子
    @Override
    public Response<?> unCollectPost(UnCollectPostReqVO unCollectPostReqVO) {
        // 帖子ID
        Long postId = unCollectPostReqVO.getId();
        // 1. 校验被收藏的帖子是否存在，若存在，则获取发布者用户 ID
        Long creatorId = checkPostIsExistAndGetCreatorId(postId);
        // 2.校验帖子是否被收藏过
        // 当前登录用户ID
        Long userId = LoginUserContextHolder.getUserId();
        // 布隆过滤器 Key
        String bloomUserPostCollectListKey = RedisKeyConstants.buildBloomUserPostCollectListKey(userId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_post_uncollect_check.lua")));
        // 返回值类型
        script.setResultType(Long.class);
        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserPostCollectListKey), postId);
        PostUnCollectLuaResultEnum postUnCollectLuaResultEnum = PostUnCollectLuaResultEnum.valueOf(result);
        switch (Objects.requireNonNull(postUnCollectLuaResultEnum)) {
            // 布隆过滤器不存在
            case NOT_EXIST -> {
                // 异步初始化布隆过滤器
                threadPoolTaskExecutor.submit(() -> {
                    // 保底1天+随机秒数
                    long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                    batchAddPostCollect2BloomAndExpire(userId, expireSeconds, bloomUserPostCollectListKey);
                });
                // 从数据库中校验帖子是否被收藏
                int count = postCollectDOMapper.selectCountByUserIdAndPostId(userId, postId);
                // 未收藏，无法取消收藏操作，抛出业务异常
                if (count == 0) throw new BizException(ResponseCodeEnum.POST_NOT_COLLECTED);
            }
            // 布隆过滤器校验目标帖子未被收藏：判断绝对正确
            case POST_NOT_COLLECTED -> throw new BizException(ResponseCodeEnum.POST_NOT_COLLECTED);
        }
        // 3.删除 ZSET 中已收藏的帖子ID
        // 用户收藏列表 ZSet Key
        String userPostCollectZSetKey = RedisKeyConstants.buildUserPostCollectZSetKey(userId);
        redisTemplate.opsForZSet().remove(userPostCollectZSetKey, postId);
        // 4.发送 MQ，数据更新落库
        // 构建消息体 DTO
        CollectUnCollectPostMqDTO unCollectPostMqDTO = CollectUnCollectPostMqDTO.builder()
                .userId(userId)
                .postId(postId)
                .postCreatorId(creatorId)
                .type(CollectUnCollectPostTypeEnum.UN_COLLECT.getCode()) // 取消收藏帖子
                .createTime(LocalDateTime.now())
                .build();
        // 构建消息对象，并将 DTO 转成 Json 字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(gson.toJson(unCollectPostMqDTO)).build();
        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = RocketMQConstants.TOPIC_COLLECT_OR_UN_COLLECT + ":" + RocketMQConstants.TAG_UN_COLLECT;
        String hashKey = String.valueOf(userId);
        // 异步发送顺序 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【帖子取消收藏】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【帖子取消收藏】MQ 发送异常: ", throwable);
            }
        });
        return Response.success();
    }

}
