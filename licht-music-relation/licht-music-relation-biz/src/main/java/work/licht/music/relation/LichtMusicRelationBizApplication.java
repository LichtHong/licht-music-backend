package work.licht.music.relation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("work.licht.music.relation.domain.mapper")
@EnableFeignClients(basePackages = "work.licht.music")
public class LichtMusicRelationBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(LichtMusicRelationBizApplication.class, args);
    }

}
