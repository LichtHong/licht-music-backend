package work.licht.music.search.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Canal 配置读取
@ConfigurationProperties(prefix = CanalProperties.PREFIX)
@Component
@Data
public class CanalProperties {

    public static final String PREFIX = "canal";

    private String host;

    private int port;

    private String destination;

    private String dbUsername;

    private String dbPassword;

    private String subscribe;

    private int batchSize = 1000;

}