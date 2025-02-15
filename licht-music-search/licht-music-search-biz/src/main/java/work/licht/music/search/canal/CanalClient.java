package work.licht.music.search.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import work.licht.music.search.config.CanalProperties;

import java.net.InetSocketAddress;
import java.util.Objects;

// Canal 客户端
@Component
@Slf4j
public class CanalClient implements DisposableBean {

    @Resource
    private CanalProperties canalProperties;

    private CanalConnector canalConnector;

    // 实例化 Canal 链接对象
    @Bean
    public CanalConnector getCanalConnector() {
        // 创建一个 CanalConnector 实例，连接到指定的 Canal 服务端
        String host = canalProperties.getHost();
        int port = canalProperties.getPort();
        String destination = canalProperties.getDestination();
        String username = canalProperties.getDbUsername();
        String password = canalProperties.getDbPassword();
        canalConnector = CanalConnectors.newSingleConnector(new InetSocketAddress(host, port), destination, username, password);
        // 连接到 Canal 服务端
        canalConnector.connect();
        // 订阅 Canal 中的数据变化，指定要监听的数据库和表（可以使用表名、数据库名的通配符）
        canalConnector.subscribe(canalProperties.getSubscribe());
        // 回滚 Canal 消费者的位点，回滚到上次提交的消费位置
        canalConnector.rollback();
        return canalConnector;
    }

    // 在 Spring 容器销毁时释放资源
    @Override
    public void destroy() {
        if (Objects.nonNull(canalConnector)) {
            // 断开 canalConnector 与 Canal 服务的连接
            canalConnector.disconnect();
        }
    }
}