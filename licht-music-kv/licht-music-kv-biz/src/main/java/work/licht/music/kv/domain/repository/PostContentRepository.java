package work.licht.music.kv.domain.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import work.licht.music.kv.domain.model.PostContentDO;

import java.util.UUID;

public interface PostContentRepository extends CassandraRepository<PostContentDO, UUID> {

}