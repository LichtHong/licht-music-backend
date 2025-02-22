package work.licht.music.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LichtMusicSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(LichtMusicSearchApplication.class, args);
    }

}