package work.licht.music.sync.constant;

// RocketMQ 常量
public interface RocketMQConstants {

    // Topic: 计数 - 帖子点赞数
    String TOPIC_COUNT_POST_LIKE = "CountPostLikeTopic";

    // Topic: 计数 - 帖子收藏数
    String TOPIC_COUNT_POST_COLLECT = "CountPostCollectTopic";

    // Topic: 帖子操作（发布、删除）
    String TOPIC_POST_OPERATE = "PostOperateTopic";

    // Topic: 关注数计数
    String TOPIC_COUNT_FOLLOWING = "CountFollowingTopic";

}