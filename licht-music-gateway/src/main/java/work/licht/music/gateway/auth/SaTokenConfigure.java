package work.licht.music.gateway.auth;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class SaTokenConfigure {
    // 注册 Sa-Token全局过滤器
    @Bean
    public SaReactorFilter getSaReactorFilter() {
        SaReactorFilter saReactorFilter = new SaReactorFilter();
        // 拦截地址
        saReactorFilter.addInclude("/**");
        // 鉴权方法：每次访问进入
        saReactorFilter.setAuth(obj -> {
            log.info("==================> SaReactorFilter, Path: {}", SaHolder.getRequest().getRequestPath());
            // 登录校验
            SaRouter.match("/**") // 拦截所有路由
                    .notMatch("/auth/login") // 排除登录接口
                    .notMatch("/auth/verification/code/send") // 排除验证码发送接口
                    .notMatch("/post/detail") // 排除帖子详情接口
                    .check(r -> StpUtil.checkLogin()); // 校验是否登录
            // TODO：权限认证
            SaRouter.match("/auth/logout", r -> StpUtil.checkPermission("app:post:publish"));
        });
        // 异常处理方法：每次setAuth函数出现异常时进入
        saReactorFilter.setError(e -> {
            // 手动抛出异常，抛给全局异常处理器
            if (e instanceof NotLoginException) { // 未登录异常
                throw new NotLoginException(e.getMessage(), null, null);
            } else if (e instanceof NotPermissionException || e instanceof NotRoleException) { // 权限不足，或不具备角色，统一抛出权限不足异常
                throw new NotPermissionException(e.getMessage());
            } else { // 其他异常，则抛出一个运行时异常
                throw new RuntimeException(e.getMessage());
            }
        });
        return saReactorFilter;
    }
}