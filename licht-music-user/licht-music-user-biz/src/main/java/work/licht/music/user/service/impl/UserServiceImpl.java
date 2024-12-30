package work.licht.music.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import work.licht.music.common.enums.DeletedEnum;
import work.licht.music.common.enums.StatusEnum;
import work.licht.music.common.exception.BizException;
import work.licht.music.common.response.Response;
import work.licht.music.common.util.ParamUtils;
import work.licht.music.context.holder.LoginUserContextHolder;
import work.licht.music.user.constant.RedisKeyConstants;
import work.licht.music.user.constant.RoleConstants;
import work.licht.music.user.domain.mapper.RoleDOMapper;
import work.licht.music.user.domain.mapper.UserDOMapper;
import work.licht.music.user.domain.mapper.UserRoleDOMapper;
import work.licht.music.user.domain.model.RoleDO;
import work.licht.music.user.domain.model.UserDO;
import work.licht.music.user.domain.model.UserRoleDO;
import work.licht.music.user.enums.ResponseCodeEnum;
import work.licht.music.user.enums.SexEnum;
import work.licht.music.user.model.dto.req.*;
import work.licht.music.user.model.dto.resp.FindUserByIdRespDTO;
import work.licht.music.user.model.dto.resp.FindUserByPhoneRespDTO;
import work.licht.music.user.model.vo.UpdateUserInfoReqVO;
import work.licht.music.user.rpc.IdRpcService;
import work.licht.music.user.rpc.OssRpcService;
import work.licht.music.user.service.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private UserDOMapper userDOMapper;
    @Resource
    private RoleDOMapper roleDOMapper;
    @Resource
    private UserRoleDOMapper userRoleDOMapper;
    @Resource
    private OssRpcService ossRpcService;
    @Resource
    private IdRpcService idRpcService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private Gson gson;

    // 用户信息本地缓存
    private static final Cache<Long, FindUserByIdRespDTO> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000) // 设置初始容量为 10000 个条目
            .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
            .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后 1 小时过期
            .build();

    // 更新用户信息
    @Override
    public Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO) {
        UserDO userDO = new UserDO();
        // 设置当前需要更新的用户 ID
        userDO.setId(LoginUserContextHolder.getUserId());
        // 标识位：是否需要更新
        boolean needUpdate = false;
        // 头像
        MultipartFile avatarFile = updateUserInfoReqVO.getAvatar();
        if (Objects.nonNull(avatarFile)) {
            String avatar = ossRpcService.uploadFile(avatarFile);
            log.info("==> 调用 oss 服务成功，上传头像，url：{}", avatar);
            // 若上传头像失败，则抛出业务异常
            if (StringUtils.isBlank(avatar)) throw new BizException(ResponseCodeEnum.UPLOAD_AVATAR_FAIL);
            userDO.setAvatar(avatar);
            needUpdate = true;
        }
        // 昵称
        String nickname = updateUserInfoReqVO.getNickname();
        if (StringUtils.isNotBlank(nickname)) {
            Preconditions.checkArgument(ParamUtils.checkNickname(nickname), ResponseCodeEnum.NICK_NAME_VALID_FAIL.getErrorMessage());
            userDO.setNickname(nickname);
            needUpdate = true;
        }
        // 账号
        String username = updateUserInfoReqVO.getUsername();
        if (StringUtils.isNotBlank(username)) {
            Preconditions.checkArgument(ParamUtils.checkUsername(username), ResponseCodeEnum.USERNAME_VALID_FAIL.getErrorMessage());
            userDO.setUsername(username);
            needUpdate = true;
        }
        // 性别
        Integer sex = updateUserInfoReqVO.getSex();
        if (Objects.nonNull(sex)) {
            Preconditions.checkArgument(SexEnum.isValid(sex), ResponseCodeEnum.SEX_VALID_FAIL.getErrorMessage());
            userDO.setSex(sex);
            needUpdate = true;
        }
        // 生日
        LocalDate birthday = updateUserInfoReqVO.getBirthday();
        if (Objects.nonNull(birthday)) {
            userDO.setBirthday(birthday);
            needUpdate = true;
        }
        // 个人简介
        String introduction = updateUserInfoReqVO.getIntroduction();
        if (StringUtils.isNotBlank(introduction)) {
            Preconditions.checkArgument(ParamUtils.checkLength(introduction, 100), ResponseCodeEnum.INTRODUCTION_VALID_FAIL.getErrorMessage());
            userDO.setIntroduction(introduction);
            needUpdate = true;
        }
        // 背景图
        MultipartFile backgroundImgFile = updateUserInfoReqVO.getBackgroundImg();
        if (Objects.nonNull(backgroundImgFile)) {
            String backgroundImg = ossRpcService.uploadFile(backgroundImgFile);
            log.info("==> 调用 oss 服务成功，上传背景图，url：{}", backgroundImg);
            // 若上传背景图失败，则抛出业务异常
            if (StringUtils.isBlank(backgroundImg)) throw new BizException(ResponseCodeEnum.UPLOAD_BACKGROUND_IMG_FAIL);
            userDO.setBackgroundImg(backgroundImg);
            needUpdate = true;
        }
        if (needUpdate) {
            // 更新用户信息
            userDO.setUpdateTime(LocalDateTime.now());
            userDOMapper.updateByPrimaryKeySelective(userDO);
        }
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Long> register(RegisterUserReqDTO registerUserReqDTO) {
        String phone = registerUserReqDTO.getPhone();
        // 先判断该手机号是否已被注册
        UserDO user1 = userDOMapper.selectByPhone(phone);
        log.info("==> 用户是否注册, phone: {}, userDO: {}", phone, gson.toJson(user1));
        // 若已注册，则直接返回用户 ID
        if (Objects.nonNull(user1)) {
            return Response.success(user1.getId());
        }
        // RPC: 调用分布式ID生成服务生成ID
        String id = idRpcService.getUserId();
        // RPC: 调用分布式ID生成服务生成后缀
        String nameSuffix = idRpcService.getUserNameSuffix();
        UserDO userDO = UserDO.builder().id(Long.valueOf(id)).phone(phone).username("id_" + nameSuffix) // 自动生成账号
                .nickname("Licht" + nameSuffix) // 自动生成昵称
                .status(StatusEnum.ENABLE.getValue()) // 状态为启用
                .createTime(LocalDateTime.now()).updateTime(LocalDateTime.now()).isDeleted(DeletedEnum.NO.getValue()) // 逻辑删除
                .build();
        // 添加入库
        userDOMapper.insert(userDO);
        // 获取刚刚添加入库的用户 ID
        Long userId = userDO.getId();
        // 给该用户分配一个默认角色
        UserRoleDO userRoleDO = UserRoleDO.builder().userId(userId).roleId(RoleConstants.COMMON_USER_ROLE_ID).createTime(LocalDateTime.now()).updateTime(LocalDateTime.now()).isDeleted(DeletedEnum.NO.getValue()).build();
        userRoleDOMapper.insert(userRoleDO);
        // 将该用户的角色 存入 Redis 中
        RoleDO roleDO = roleDOMapper.selectByPrimaryKey(RoleConstants.COMMON_USER_ROLE_ID);
        List<String> roles = new ArrayList<>(1);
        roles.add(roleDO.getRoleKey());
        String userRolesKey = RedisKeyConstants.buildUserRoleKey(userId);
        redisTemplate.opsForValue().set(userRolesKey, gson.toJson(roles));
        return Response.success(userId);
    }

    // 根据用户 ID 查询用户信息
    @Override
    public Response<FindUserByIdRespDTO> findById(FindUserByIdReqDTO findUserByIdReqDTO) {
        Long userId = findUserByIdReqDTO.getId();
        // 先从本地缓存中查询
        FindUserByIdRespDTO findUserByIdRspDTOLocalCache = LOCAL_CACHE.getIfPresent(userId);
        if (Objects.nonNull(findUserByIdRspDTOLocalCache)) {
            log.info("==> 命中了本地缓存；{}", findUserByIdRspDTOLocalCache);
            return Response.success(findUserByIdRspDTOLocalCache);
        }
        // 用户缓存 Redis Key
        String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(userId);
        // 先从 Redis 缓存中查询
        String userInfoRedisValue = (String) redisTemplate.opsForValue().get(userInfoRedisKey);
        // 若 Redis 缓存中存在该用户信息
        if (StringUtils.isNotBlank(userInfoRedisValue)) {
            // 将存储的 Json 字符串转换成对象，并返回
            FindUserByIdRespDTO findUserByIdRespDTO = gson.fromJson(userInfoRedisValue, FindUserByIdRespDTO.class);
            // 异步线程中将用户信息存入本地缓存
            threadPoolTaskExecutor.submit(() -> {
                if (Objects.nonNull(findUserByIdRespDTO)) {
                    // 写入本地缓存
                    LOCAL_CACHE.put(userId, findUserByIdRespDTO);
                }
            });
            return Response.success(findUserByIdRespDTO);
        }
        // 否则, 从数据库中查询
        // 根据用户 ID 查询用户信息
        UserDO userDO = userDOMapper.selectByPrimaryKey(userId);
        // 判空
        if (Objects.isNull(userDO)) throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        // 构建返参
        FindUserByIdRespDTO findUserByIdRspDTO = FindUserByIdRespDTO.builder()
                .id(userDO.getId())
                .nickName(userDO.getNickname())
                .avatar(userDO.getAvatar())
                .introduction(userDO.getIntroduction())
                .build();
        // 异步将用户信息存入 Redis 缓存，提升响应速度
        threadPoolTaskExecutor.submit(() -> {
            // 过期时间（保底1天 + 随机秒数，将缓存过期时间打散，防止同一时间大量缓存失效，导致数据库压力太大）
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            redisTemplate.opsForValue().set(userInfoRedisKey, gson.toJson(findUserByIdRspDTO), expireSeconds, TimeUnit.SECONDS);
        });
        return Response.success(findUserByIdRspDTO);
    }

    @Override
    public Response<FindUserByPhoneRespDTO> findByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO) {
        String phone = findUserByPhoneReqDTO.getPhone();
        // 根据手机号查询用户信息
        UserDO userDO = userDOMapper.selectByPhone(phone);
        // 判空
        if (Objects.isNull(userDO)) throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        // 构建返参
        FindUserByPhoneRespDTO findUserByPhoneRspDTO = FindUserByPhoneRespDTO.builder()
                .id(userDO.getId()).password(userDO.getPassword()).build();
        return Response.success(findUserByPhoneRspDTO);
    }

    /**
     * 批量根据用户 ID 查询用户信息
     */
    @Override
    public Response<List<FindUserByIdRespDTO>> findByIds(FindUsersByIdsReqDTO findUsersByIdsReqDTO) {
        // 需要查询的用户 ID 集合
        List<Long> userIds = findUsersByIdsReqDTO.getIds();
        // 构建 Redis Key 集合
        List<String> redisKeys = userIds.stream()
                .map(RedisKeyConstants::buildUserInfoKey)
                .toList();
        // 先从 Redis 缓存中查, multiGet 批量查询提升性能
        List<Object> redisValues = redisTemplate.opsForValue().multiGet(redisKeys);
        // 如果缓存中不为空
        if (CollUtil.isNotEmpty(redisValues)) {
            // 过滤掉为空的数据
            redisValues = redisValues.stream().filter(Objects::nonNull).toList();
        }
        // 返参
        List<FindUserByIdRespDTO> findUserByIdRspDTOS = Lists.newArrayList();
        // 将过滤后的缓存集合，转换为 DTO 返参实体类
        if (CollUtil.isNotEmpty(redisValues)) {
            findUserByIdRspDTOS = redisValues.stream()
                    .map(value -> gson.fromJson(String.valueOf(value), FindUserByIdRespDTO.class))
                    .collect(Collectors.toList());
        }
        // 如果被查询的用户信息，都在 Redis 缓存中, 则直接返回
        if (CollUtil.size(userIds) == CollUtil.size(findUserByIdRspDTOS)) {
            return Response.success(findUserByIdRspDTOS);
        }
        // 筛选出缓存里没有的用户数据，去查数据库
        List<Long> userIdsNeedQuery = null;
        if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
            // 将 findUserInfoByIdRspDTOS 集合转 Map
            Map<Long, FindUserByIdRespDTO> map = findUserByIdRspDTOS.stream()
                    .collect(Collectors.toMap(FindUserByIdRespDTO::getId, p -> p));
            // 筛选出需要查 DB 的用户 ID
            userIdsNeedQuery = userIds.stream()
                    .filter(id -> Objects.isNull(map.get(id)))
                    .toList();
        } else {
            // 缓存中一条用户信息都没查到，则提交的用户 ID 集合都需要查数据库
            userIdsNeedQuery = userIds;
        }
        // 从数据库中批量查询
        List<UserDO> userDOS = userDOMapper.selectByIds(userIdsNeedQuery);
        List<FindUserByIdRespDTO> findUserByIdRspDTOS2 = null;
        // 若数据库查询的记录不为空
        if (CollUtil.isNotEmpty(userDOS)) {
            // DO 转 DTO
            findUserByIdRspDTOS2 = userDOS.stream()
                    .map(userDO -> FindUserByIdRespDTO.builder()
                            .id(userDO.getId())
                            .nickName(userDO.getNickname())
                            .avatar(userDO.getAvatar())
                            .introduction(userDO.getIntroduction())
                            .build())
                    .collect(Collectors.toList());
            // 异步线程将用户信息同步到 Redis 中
            List<FindUserByIdRespDTO> finalFindUserByIdRspDTOS = findUserByIdRspDTOS2;
            threadPoolTaskExecutor.submit(() -> {
                // DTO 集合转 Map
                Map<Long, FindUserByIdRespDTO> map = finalFindUserByIdRspDTOS.stream()
                        .collect(Collectors.toMap(FindUserByIdRespDTO::getId, p -> p));
                // 执行 pipeline 操作
                redisTemplate.executePipelined((RedisCallback<Void>) connection -> {
                    for (UserDO userDO : userDOS) {
                        Long userId = userDO.getId();
                        // 用户信息缓存 Redis Key
                        String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(userId);
                        // DTO 转 JSON 字符串
                        FindUserByIdRespDTO findUserInfoByIdRspDTO = map.get(userId);
                        String value = gson.toJson(findUserInfoByIdRspDTO);
                        // 过期时间（保底1天 + 随机秒数，将缓存过期时间打散，防止同一时间大量缓存失效，导致数据库压力太大）
                        long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                        redisTemplate.opsForValue().set(userInfoRedisKey, value, expireSeconds, TimeUnit.SECONDS);
                    }
                    return null;
                });
            });
        }
        // 合并数据
        if (CollUtil.isNotEmpty(findUserByIdRspDTOS2)) {
            findUserByIdRspDTOS.addAll(findUserByIdRspDTOS2);
        }
        return Response.success(findUserByIdRspDTOS);
    }

    @Override
    public Response<?> updatePassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO) {
        // 获取当前请求对应的用户 ID
        Long userId = LoginUserContextHolder.getUserId();
        UserDO userDO = UserDO.builder().id(userId).password(updateUserPasswordReqDTO.getEncodePassword()) // 加密后的密码
                .updateTime(LocalDateTime.now()).build();
        // 更新密码
        userDOMapper.updateByPrimaryKeySelective(userDO);
        return Response.success();
    }
}
