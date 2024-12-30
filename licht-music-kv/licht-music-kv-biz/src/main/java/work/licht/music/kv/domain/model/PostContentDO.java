package work.licht.music.kv.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Table("post_content")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostContentDO {

    @PrimaryKey("id")
    private UUID id;

    private String content;
}