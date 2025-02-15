package work.licht.music.sync;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("work.licht.music.sync.domain.mapper")
public class LichtMusicSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(LichtMusicSyncApplication.class, args);
    }

}