package work.licht.music.post;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("work.licht.music.post.domain.mapper")
@EnableFeignClients(basePackages = "work.licht.music")
public class LichtMusicPostBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(LichtMusicPostBizApplication.class, args);
    }

}