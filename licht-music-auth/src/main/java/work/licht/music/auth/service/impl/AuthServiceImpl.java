package work.licht.music.auth.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.google.common.base.Preconditions;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import work.licht.music.auth.constant.RedisKeyConstants;
import work.licht.music.auth.model.vo.user.UpdatePasswordReqVO;
import work.licht.music.auth.rpc.UserRpcService;
import work.licht.music.auth.enums.LoginTypeEnum;
import work.licht.music.auth.enums.ResponseCodeEnum;
import work.licht.music.auth.model.vo.user.UserLoginReqVO;
import work.licht.music.auth.service.AuthService;
import work.licht.music.common.exception.BizException;
import work.licht.music.common.response.Response;
import work.licht.music.context.holder.LoginUserContextHolder;
import work.licht.music.user.model.dto.resp.FindUserByPhoneRespDTO;

import java.util.Objects;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Resource
    private UserRpcService userRpcService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private PasswordEncoder passwordEncoder;

    // 登录与注册
    @Override
    public Response<String> loginAndRegister(UserLoginReqVO userLoginReqVO) {
        String phone = userLoginReqVO.getPhone();
        Integer type = userLoginReqVO.getType();
        LoginTypeEnum loginTypeEnum = LoginTypeEnum.valueOf(type);
        Long userId = null;
        // 判断登录类型
        if (loginTypeEnum != null) {
            switch (loginTypeEnum) {
                case VERIFICATION_CODE: // 验证码登录
                    String verificationCode = userLoginReqVO.getCode();
                    Preconditions.checkArgument(StringUtils.isNotBlank(verificationCode), "验证码不能为空"); // 校验入参验证码是否为空
                    // 构建验证码 Redis Key
                    String key = RedisKeyConstants.buildVerificationCodeKey(phone);
                    // 查询存储在 Redis 中该用户的登录验证码
                    String sentCode = (String) redisTemplate.opsForValue().get(key);
                    // 判断用户提交的验证码，与 Redis 中的验证码是否一致
                    if (!StringUtils.equals(verificationCode, sentCode)) {
                        throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_ERROR);
                    }
                    // RPC: 调用用户服务，注册用户
                    Long userIdTmp = userRpcService.registerUser(phone);
                    // 若调用用户服务，返回的用户 ID 为空，则提示登录失败
                    if (Objects.isNull(userIdTmp)) {
                        throw new BizException(ResponseCodeEnum.LOGIN_FAIL);
                    }
                    userId = userIdTmp;
                    break;
                case PASSWORD: // 密码登录
                    String password = userLoginReqVO.getPassword();
                    // RPC: 调用用户服务，通过手机号查询用户
                    FindUserByPhoneRespDTO findUserByPhoneRespDTO = userRpcService.selectUserByPhone(phone);
                    // 判断该手机号是否注册
                    if (Objects.isNull(findUserByPhoneRespDTO)) {
                        throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
                    }
                    // 拿到密文密码
                    String encodePassword = findUserByPhoneRespDTO.getPassword();
                    // 匹配密码是否一致
                    boolean isPasswordCorrect = passwordEncoder.matches(password, encodePassword);
                    // 如果不正确，则抛出业务异常，提示用户名或者密码不正确
                    if (!isPasswordCorrect) {
                        throw new BizException(ResponseCodeEnum.PHONE_OR_PASSWORD_ERROR);
                    }
                    userId = findUserByPhoneRespDTO.getId();
                    break;
                default:
                    break;
            }
        }
        StpUtil.login(userId);
        // 获取 Token 令牌
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        // 返回 Token 令牌
        return Response.success(tokenInfo.tokenValue);
    }

    @Override
    public Response<?> logout() {
        Long userId = LoginUserContextHolder.getUserId();
        log.info("==> 用户退出登录, userId: {}", userId);
        // 退出登录 (指定用户 ID)
        StpUtil.logout(userId);
        return Response.success();
    }

    // 修改密码
    @Override
    public Response<?> updatePassword(UpdatePasswordReqVO updatePasswordReqVO) {
        // 新密码
        String newPassword = updatePasswordReqVO.getNewPassword();
        // 密码加密
        String encodePassword = passwordEncoder.encode(newPassword);
        // RPC: 调用用户服务：更新密码
        userRpcService.updatePassword(encodePassword);
        return Response.success();
    }

}