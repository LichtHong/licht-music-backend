package work.licht.music.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import work.licht.music.auth.constant.RedisKeyConstants;
import work.licht.music.auth.enums.ResponseCodeEnum;
import work.licht.music.auth.model.vo.verificationcode.SendVerificationCodeReqVO;
import work.licht.music.auth.service.VerificationCodeService;
import work.licht.music.auth.sms.AliyunSmsHelper;
import work.licht.music.common.exception.BizException;
import work.licht.music.common.response.Response;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class VerificationCodeServiceImpl implements VerificationCodeService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private AliyunSmsHelper aliyunSmsHelper;

    // 发送短信验证码
    @Override
    public Response<?> send(SendVerificationCodeReqVO sendVerificationCodeReqVO) {
        // 手机号
        String phone = sendVerificationCodeReqVO.getPhone();

        // 构建验证码 redis key
        String key = RedisKeyConstants.buildVerificationCodeKey(phone);

        // 判断是否已发送验证码
        boolean isSent = Boolean.TRUE.equals(redisTemplate.hasKey(key));
        if (isSent) {
            // 若之前发送的验证码未过期，则提示发送频繁
            throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_SEND_FREQUENTLY);
        }

        // 生成 6 位随机数字验证码
        String verificationCode = RandomUtil.randomNumbers(6);
        log.info("==> 手机号: {}, 已生成验证码：【{}】", phone, verificationCode);

        // 调用第三方短信发送服务
        /*threadPoolTaskExecutor.submit(() -> {
            String signName = "Licht音乐";
            String templateCode = "SMS_475140556";
            String templateParam = String.format("{\"code\":\"%s\"}", verificationCode);
            aliyunSmsHelper.sendMessage(signName, templateCode, phone, templateParam);
        });*/

        // 存储验证码到 redis, 并设置过期时间为 5 分钟
        redisTemplate.opsForValue().set(key, verificationCode, 5, TimeUnit.MINUTES);

        return Response.success();
    }
}