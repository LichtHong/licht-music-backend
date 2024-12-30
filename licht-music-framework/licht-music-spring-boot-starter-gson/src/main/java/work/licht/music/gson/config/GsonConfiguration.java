package work.licht.music.gson.config;

import com.google.gson.ToNumberPolicy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import work.licht.music.gson.adapter.LocalDateJsonAdapter;
import work.licht.music.gson.adapter.LocalDateTimeJsonAdapter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AutoConfiguration
public class GsonConfiguration {

    @Bean
    public GsonBuilderCustomizer gsonBuilderCustomizer() {
        return builder -> {
            builder.setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER);
            builder.registerTypeAdapter(LocalDate.class, new LocalDateJsonAdapter());
            builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeJsonAdapter());
        };
    }
}
