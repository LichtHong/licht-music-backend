package work.licht.music.count;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("work.licht.music.count.domain.mapper")
public class LichtMusicCountBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(LichtMusicCountBizApplication.class, args);
    }

}
