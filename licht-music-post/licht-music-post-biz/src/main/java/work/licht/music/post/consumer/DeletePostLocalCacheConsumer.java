package work.licht.music.post.consumer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import work.licht.music.post.constant.RocketMQConstants;
import work.licht.music.post.service.PostService;

@Component
@Slf4j
@RocketMQMessageListener(
        consumerGroup = "licht_music_post_group", // 消费者组
        topic = RocketMQConstants.TOPIC_DELETE_POST_LOCAL_CACHE, // 消费主题
        messageModel = MessageModel.BROADCASTING // 广播模式
)
public class DeletePostLocalCacheConsumer implements RocketMQListener<String> {

    @Resource
    PostService postService;

    @Override
    public void onMessage(String body) {
        Long postId = Long.valueOf(body);
        postService.deletePostLocalCache(postId);
        log.info("## 消费者消费成功, postId: {}", postId);
    }

}