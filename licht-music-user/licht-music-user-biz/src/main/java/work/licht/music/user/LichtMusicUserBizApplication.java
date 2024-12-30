package work.licht.music.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("work.licht.music.user.domain.mapper")
@EnableFeignClients(basePackages = "work.licht.music")
public class LichtMusicUserBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(LichtMusicUserBizApplication.class, args);
    }

}