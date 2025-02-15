package work.licht.music.search.elasticsearch;

import jakarta.annotation.Resource;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import work.licht.music.search.config.ElasticsearchProperties;

// ElasticsearchRestClient 客户端
@Configuration
public class ElasticsearchRestHighLevelClient {

    @Resource
    private ElasticsearchProperties elasticsearchProperties;

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        String host = elasticsearchProperties.getHost();
        int port = elasticsearchProperties.getPort();
        HttpHost httpHost = new HttpHost(host, port, "http");
        return new RestHighLevelClient(RestClient.builder(httpHost));
    }
}