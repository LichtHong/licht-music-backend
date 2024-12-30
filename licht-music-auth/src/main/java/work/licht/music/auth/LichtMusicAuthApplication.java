package work.licht.music.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "work.licht.music")
public class LichtMusicAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(LichtMusicAuthApplication.class, args);
    }

}
