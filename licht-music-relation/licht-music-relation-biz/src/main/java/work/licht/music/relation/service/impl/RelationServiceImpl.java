package work.licht.music.relation.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.google.gson.Gson;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
import work.licht.music.common.exception.BizException;
import work.licht.music.common.response.PageResponse;
import work.licht.music.common.response.Response;
import work.licht.music.common.util.DateUtils;
import work.licht.music.context.holder.LoginUserContextHolder;
import work.licht.music.relation.constant.RedisKeyConstants;
import work.licht.music.relation.constant.RocketMQConstants;
import work.licht.music.relation.domain.mapper.FanDOMapper;
import work.licht.music.relation.domain.mapper.FollowerDOMapper;
import work.licht.music.relation.domain.model.FanDO;
import work.licht.music.relation.domain.model.FollowerDO;
import work.licht.music.relation.enums.LuaResultEnum;
import work.licht.music.relation.enums.ResponseCodeEnum;
import work.licht.music.relation.model.vo.*;
import work.licht.music.relation.rpc.UserRpcService;
import work.licht.music.relation.service.RelationService;
import work.licht.music.user.model.dto.resp.FindUserByIdRespDTO;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class RelationServiceImpl implements RelationService {

    @Resource
    private FanDOMapper fanDOMapper;
    @Resource
    private FollowerDOMapper followerDOMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private UserRpcService userRpcService;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private Gson gson;

    // 校验 Lua 脚本结果，根据状态码抛出对应的业务异常
    private static void checkLuaScriptResult(Long result) {
        LuaResultEnum luaResultEnum = LuaResultEnum.valueOf(result);
        if (Objects.isNull(luaResultEnum)) throw new RuntimeException("Redis Lua 返回结果错误");
        // 校验 Lua 脚本执行结果
        switch (luaResultEnum) {
            // 关注数已达到上限
            case FOLLOW_LIMIT -> throw new BizException(ResponseCodeEnum.FOLLOW_COUNT_LIMIT);
            // 已经关注了该用户
            case ALREADY_FOLLOWED -> throw new BizException(ResponseCodeEnum.ALREADY_FOLLOWED);
        }
    }

    // 构建 Lua 脚本参数
    private static Object[] buildLuaArgs(List<FollowerDO> followerDOList, long expireSeconds) {
        // 每个关注关系有 2 个参数（score 和 value），再加一个过期时间
        int argsLength = followerDOList.size() * 2 + 1;
        Object[] luaArgs = new Object[argsLength];
        int i = 0;
        for (FollowerDO followerDO : followerDOList) {
            // 关注时间作为 score
            luaArgs[i] = DateUtils.localDateTime2Timestamp(followerDO.getCreateTime());
            // 关注的用户 ID 作为 ZSet value
            luaArgs[i + 1] = followerDO.getFollowerUserId();
            i += 2;
        }
        // 最后一个参数是 ZSet 的过期时间
        luaArgs[argsLength - 1] = expireSeconds;
        return luaArgs;
    }

    // 关注用户
    @Override
    public Response<?> follow(FollowUserReqVO followUserReqVO) {
        // 关注的用户 ID
        Long followUserId = followUserReqVO.getFollowUserId();
        // 当前登录的用户 ID
        Long userId = LoginUserContextHolder.getUserId();
        // 校验：无法关注自己
        if (Objects.equals(userId, followUserId)) throw new BizException(ResponseCodeEnum.CANT_FOLLOW_YOUR_SELF);
        // 校验：关注的用户是否存在
        FindUserByIdRespDTO findUserByIdRespDTO = userRpcService.findById(followUserId);
        if (Objects.isNull(findUserByIdRespDTO)) throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
        // 构建当前用户关注列表的 Redis Key
        String followerRedisKey = RedisKeyConstants.buildUserFollowerKey(userId);
        // Redis Lua 脚本
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_add.lua")));
        // 设置返回值类型
        script.setResultType(Long.class);
        // 当前时间
        LocalDateTime now = LocalDateTime.now();
        // 当前时间转时间戳
        long timestamp = DateUtils.localDateTime2Timestamp(now);
        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(followerRedisKey), followUserId, timestamp);
        // 校验 Lua 脚本执行结果
        checkLuaScriptResult(result);
        // ZSET 不存在
        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) {
            // 从数据库查询当前用户的关注关系记录
            List<FollowerDO> followerDOList = followerDOMapper.selectByUserId(userId);
            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            // 若记录为空，直接 ZADD 对象, 并设置过期时间
            if (CollUtil.isEmpty(followerDOList)) {
                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_add_and_expire.lua")));
                script2.setResultType(Long.class);
                // TODO: 可以根据用户类型，设置不同的过期时间，若当前用户为大V, 则可以过期时间设置的长些或者不设置过期时间；如不是，则设置的短些
                redisTemplate.execute(script2, Collections.singletonList(followerRedisKey), followUserId, timestamp, expireSeconds);
            } else {
                // 若记录不为空，则将关注关系数据全量同步到 Redis 中，并设置过期时间
                // 构建 Lua 参数
                Object[] luaArgs = buildLuaArgs(followerDOList, expireSeconds);
                // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
                DefaultRedisScript<Long> script3 = new DefaultRedisScript<>();
                script3.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
                script3.setResultType(Long.class);
                redisTemplate.execute(script3, Collections.singletonList(followerRedisKey), luaArgs);
                // 再次调用上面的 Lua 脚本：follow_check_and_add.lua , 将最新的关注关系添加进去
                result = redisTemplate.execute(script, Collections.singletonList(followerRedisKey), followUserId, timestamp);
                checkLuaScriptResult(result);
            }
        }
        // 发送 MQ
        // 构建消息体 DTO
        FollowUserMqDTO followUserMqDTO = FollowUserMqDTO.builder()
                .userId(userId)
                .followUserId(followUserId)
                .createTime(now)
                .build();
        // 构建消息对象，并将 DTO 转成 Json 字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(gson.toJson(followUserMqDTO)).build();
        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = RocketMQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + RocketMQConstants.TAG_FOLLOW;
        log.info("==> RELATION FOLLOW: 开始发送关注操作 MQ, 消息体: {}", followUserMqDTO);
        String hashKey = String.valueOf(userId);
        // 异步发送 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> RELATION FOLLOW: MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> RELATION FOLLOW: MQ 发送异常: ", throwable);
            }
        });
        return Response.success();
    }

    // 取关用户
    @Override
    public Response<?> unfollow(UnfollowUserReqVO unfollowUserReqVO) {
        // 取关用户 ID
        Long unfollowUserId = unfollowUserReqVO.getUnfollowUserId();
        // 当前登录用户 ID
        Long userId = LoginUserContextHolder.getUserId();
        // 无法取关自己
        if (Objects.equals(userId, unfollowUserId)) throw new BizException(ResponseCodeEnum.CANT_UNFOLLOW_YOUR_SELF);
        // 校验关注的用户是否存在
        FindUserByIdRespDTO findUserByIdRespDTO = userRpcService.findById(unfollowUserId);
        if (Objects.isNull(findUserByIdRespDTO)) throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
        // 当前用户的关注列表 Redis Key
        String followerRedisKey = RedisKeyConstants.buildUserFollowerKey(userId);
        // Lua 脚本
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/unfollow_check_and_delete.lua")));
        // 返回值类型
        script.setResultType(Long.class);
        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(followerRedisKey), unfollowUserId);
        // 校验 Lua 脚本执行结果
        // 取关的用户不在关注列表中
        if (Objects.equals(result, LuaResultEnum.NOT_FOLLOWED.getCode()))
            throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) { // ZSET 关注列表不存在
            // 从数据库查询当前用户的关注关系记录
            List<FollowerDO> followerDOList = followerDOMapper.selectByUserId(userId);
            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            // 若记录为空，则表示还未关注任何人，提示还未关注对方
            if (CollUtil.isEmpty(followerDOList)) throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
            // 若记录不为空，则将关注关系数据全量同步到 Redis 中，并设置过期时间；
            // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
            DefaultRedisScript<Long> script3 = new DefaultRedisScript<>();
            script3.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
            script3.setResultType(Long.class);
            // 构建 Lua 参数
            Object[] luaArgs = buildLuaArgs(followerDOList, expireSeconds);
            redisTemplate.execute(script3, Collections.singletonList(followerRedisKey), luaArgs);
            // 再次调用上面的 Lua 脚本：unfollow_check_and_delete.lua , 将取关的用户删除
            result = redisTemplate.execute(script, Collections.singletonList(followerRedisKey), unfollowUserId);
            // 再次校验结果
            if (Objects.equals(result, LuaResultEnum.NOT_FOLLOWED.getCode()))
                throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
        }
        // 发送 MQ
        // 构建消息体 DTO
        UnfollowUserMqDTO unfollowUserMqDTO = UnfollowUserMqDTO.builder()
                .userId(userId)
                .unfollowUserId(unfollowUserId)
                .createTime(LocalDateTime.now())
                .build();
        // 构建消息对象，并将 DTO 转成 Json 字符串设置到消息体中
        Message<String> message = MessageBuilder.withPayload(gson.toJson(unfollowUserMqDTO)).build();
        // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
        String destination = RocketMQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + RocketMQConstants.TAG_UNFOLLOW;
        log.info("==> RELATION UNFOLLOW: 开始发送取关操作 MQ, 消息体: {}", unfollowUserMqDTO);
        String hashKey = String.valueOf(userId);
        // 异步发送 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> RELATION UNFOLLOW: MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> RELATION UNFOLLOW: MQ 发送异常: ", throwable);
            }
        });
        return Response.success();
    }

    @Override
    public PageResponse<FindFollowerUserRespVO> findFollowerList(FindFollowerListReqVO findFollowerListReqVO) {
        // 返参
        List<FindFollowerUserRespVO> findFollowerUserRespVOS = null;
        // 想要查询的用户 ID
        Long userId = findFollowerListReqVO.getUserId();
        // 页码
        Integer pageNo = findFollowerListReqVO.getPageNo();
        // 先从 Redis 中查询
        String followerListRedisKey = RedisKeyConstants.buildUserFollowerKey(userId);
        // 查询目标用户关注列表 ZSet 的总大小
        // 每页展示 10 条数据
        long limit = 10;
        long total = redisTemplate.opsForZSet().zCard(followerListRedisKey);
        if (total > 0) { // 缓存中有数据
            // 计算一共多少页
            long totalPage = PageResponse.getTotalPage(total, limit);
            // 请求的页码超出了总页数
            if (pageNo > totalPage) return PageResponse.success(new ArrayList<>(), pageNo, total);
            // 准备从 Redis 中查询 ZSet 分页数据
            // 每页 10 个元素，计算偏移量
            long offset = (pageNo - 1) * limit;
            // 使用 ZREVRANGEBYSCORE 命令按 score 降序获取元素，同时使用 LIMIT 子句实现分页
            // 注意：这里使用了 Double.POSITIVE_INFINITY 和 Double.NEGATIVE_INFINITY 作为分数范围
            // 因为关注列表最多有 1000 个元素，这样可以确保获取到所有的元素
            Set<Object> followerUserIdSet = redisTemplate.opsForZSet()
                    .reverseRangeByScore(followerListRedisKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, limit);
            if (CollUtil.isNotEmpty(followerUserIdSet)) {
                // 提取所有用户 ID 到集合中
                List<Long> userIdList = followerUserIdSet.stream().map(object -> ((Number) object).longValue()).toList();
                // RPC: 批量查询用户信息
                findFollowerUserRespVOS = rpcUserServiceAndDTO2FollowerVO(userIdList);
            }
        } else {
            // 若 Redis 中没有数据，则从数据库查询
            // 先查询记录总量
            long count = followerDOMapper.selectCountByUserId(userId);
            // 计算一共多少页
            long totalPage = PageResponse.getTotalPage(count, limit);
            // 请求的页码超出了总页数
            if (pageNo > totalPage) return PageResponse.success(new ArrayList<>(), pageNo, count);
            // 偏移量
            long offset = PageResponse.getOffset(pageNo, limit);
            // 分页查询
            List<FollowerDO> followerDOList = followerDOMapper.selectPageListByUserId(userId, offset, limit);
            // 若记录不为空
            if (CollUtil.isNotEmpty(followerDOList)) {
                // 提取所有关注用户 ID 到集合中
                List<Long> userIdList = followerDOList.stream().map(FollowerDO::getFollowerUserId).toList();
                // RPC: 调用用户服务，并将 DTO 转换为 VO
                findFollowerUserRespVOS = rpcUserServiceAndDTO2FollowerVO(userIdList);
                // 异步将关注列表全量同步到 Redis
                threadPoolTaskExecutor.submit(() -> syncFollowerList2Redis(userId));
            }
        }

        return PageResponse.success(findFollowerUserRespVOS, pageNo, total);
    }

    // 全量同步关注列表至 Redis 中
    private void syncFollowerList2Redis(Long userId) {
        // 查询全量关注用户列表（1000位用户）
        List<FollowerDO> followerDOList = followerDOMapper.selectAllByUserId(userId);
        if (CollUtil.isNotEmpty(followerDOList)) {
            // 用户关注列表 Redis Key
            String followerListRedisKey = RedisKeyConstants.buildUserFollowerKey(userId);
            // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
            script.setResultType(Long.class);
            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            // 构建 Lua 参数
            Object[] luaArgs = buildLuaArgs(followerDOList, expireSeconds);
            redisTemplate.execute(script, Collections.singletonList(followerListRedisKey), luaArgs);
        }
    }

    // 调用用户服务，并将 DTO 转换为 VO
    private List<FindFollowerUserRespVO> rpcUserServiceAndDTO2FollowerVO(List<Long> userIdList) {
        // RPC: 批量查询用户信息
        List<FindUserByIdRespDTO> userList = userRpcService.findByIds(userIdList);
        // 若不为空，DTO 转 VO
        if (CollUtil.isNotEmpty(userList)) {
            return userList.stream()
                    .map(dto -> FindFollowerUserRespVO.builder()
                            .userId(dto.getId())
                            .avatar(dto.getAvatar())
                            .nickname(dto.getNickName())
                            .introduction(dto.getIntroduction())
                            .build())
                    .toList();
        }
        return new ArrayList<>();
    }

    // 查询粉丝列表
    @Override
    public PageResponse<FindFanUserRespVO> findFanList(FindFanListReqVO findFanListReqVO) {
        // 想要查询的用户 ID
        Long userId = findFanListReqVO.getUserId();
        // 页码
        Integer pageNo = findFanListReqVO.getPageNo();
        // 先从 Redis 中查询
        String fanListRedisKey = RedisKeyConstants.buildUserFanKey(userId);
        // 查询目标用户粉丝列表 ZSet 的总大小
        long total = redisTemplate.opsForZSet().zCard(fanListRedisKey);
        // 返参
        List<FindFanUserRespVO> findFanUserRespVOS = null;
        // 每页展示 10 条数据
        long limit = 10;
        if (total > 0) { // 缓存中有数据
            // 计算一共多少页
            long totalPage = PageResponse.getTotalPage(total, limit);
            // 请求的页码超出了总页数
            if (pageNo > totalPage) return PageResponse.success(new ArrayList<>(), pageNo, total);
            // 准备从 Redis 中查询 ZSet 分页数据
            // 每页 10 个元素，计算偏移量
            long offset = PageResponse.getOffset(pageNo, limit);
            // 使用 ZREVRANGEBYSCORE 命令按 score 降序获取元素，同时使用 LIMIT 子句实现分页
            Set<Object> followingUserIdsSet = redisTemplate.opsForZSet()
                    .reverseRangeByScore(fanListRedisKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, limit);
            if (CollUtil.isNotEmpty(followingUserIdsSet)) {
                // 提取所有用户 ID 到集合中
                List<Long> userIds = followingUserIdsSet.stream().map(object -> ((Number) object).longValue()).toList();
                // RPC: 批量查询用户信息
                findFanUserRespVOS = rpcUserServiceAndDTO2FanVO(userIds);
            }
        } else { // 若 Redis 缓存中无数据，则查询数据库
            // 先查询记录总量
            total = fanDOMapper.selectCountByUserId(userId);
            // 计算一共多少页
            long totalPage = PageResponse.getTotalPage(total, limit);
            // 请求的页码超出了总页数（只允许查询前 500 页）
            if (pageNo > totalPage) return PageResponse.success(new ArrayList<>(), pageNo, total);
            // 偏移量
            long offset = PageResponse.getOffset(pageNo, limit);
            // 分页查询
            List<FanDO> fanDOList = fanDOMapper.selectPageListByUserId(userId, offset, limit);
            // 若记录不为空
            if (CollUtil.isNotEmpty(fanDOList)) {
                // 提取所有粉丝用户 ID 到集合中
                List<Long> userIds = fanDOList.stream().map(FanDO::getFanUserId).toList();
                // RPC: 调用用户服务、计数服务，并将 DTO 转换为 VO
                findFanUserRespVOS = rpcUserServiceAndDTO2FanVO(userIds);
                // 异步将粉丝列表同步到 Redis
                threadPoolTaskExecutor.submit(() -> syncFanList2Redis(userId));
            }
        }
        return PageResponse.success(findFanUserRespVOS, pageNo, total);
    }

    // 粉丝列表同步到 Redis
    private void syncFanList2Redis(Long userId) {
        List<FanDO> fanDOList = fanDOMapper.selectListByUserId(userId, 5000);
        if (CollUtil.isNotEmpty(fanDOList)) {
            // 用户粉丝列表 Redis Key
            String fanListRedisKey = RedisKeyConstants.buildUserFanKey(userId);
            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
            // 构建 Lua 参数
            Object[] luaArgs = buildFanZSetLuaArgs(fanDOList, expireSeconds);
            // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
            script.setResultType(Long.class);
            redisTemplate.execute(script, Collections.singletonList(fanListRedisKey), luaArgs);
        }
    }

    private static Object[] buildFanZSetLuaArgs(List<FanDO> fanDOList, long expireSeconds) {
        // 每个粉丝关系有 2 个参数（score 和 value），再加一个过期时间
        int argsLength = fanDOList.size() * 2 + 1;
        Object[] luaArgs = new Object[argsLength];
        int i = 0;
        for (FanDO fanDO : fanDOList) {
            // 粉丝的关注时间作为 score
            luaArgs[i] = DateUtils.localDateTime2Timestamp(fanDO.getCreateTime());
            // 粉丝的用户 ID 作为 ZSet value
            luaArgs[i + 1] = fanDO.getFanUserId();
            i += 2;
        }
        luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数是 ZSet 的过期时间
        return luaArgs;
    }

    // RPC: 调用用户服务、计数服务，并将 DTO 转换为 VO 粉丝列表
    private List<FindFanUserRespVO> rpcUserServiceAndDTO2FanVO(List<Long> userIdList) {
        // RPC: 批量查询用户信息
        List<FindUserByIdRespDTO> userList = userRpcService.findByIds(userIdList);
        // 若不为空，DTO 转 VO
        if (CollUtil.isNotEmpty(userList)) {
            return userList.stream()
                    .map(dto -> FindFanUserRespVO.builder()
                            .userId(dto.getId())
                            .avatar(dto.getAvatar())
                            .nickname(dto.getNickName())
                            .introduction(dto.getIntroduction())
                            .build())
                    .toList();
        }
        return new ArrayList<>();
    }
}