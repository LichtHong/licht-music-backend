package work.licht.music.count.constant;

public interface RocketMQConstants {

    // Topic：关注数计数
    String TOPIC_COUNT_FOLLOWING = "CountFollowingTopic";

    // Topic：粉丝数计数
    String TOPIC_COUNT_FOLLOWER = "CountFollowerTopic";

    // Topic：粉丝数计数入库
    String TOPIC_COUNT_FOLLOWER_2_DB = "CountFollowerDBTopic";

    // Topic：关注数计数入库
    String TOPIC_COUNT_FOLLOWING_2_DB = "CountFollowing2DBTopic";

    // Topic：计数 - 帖子点赞数
    String TOPIC_COUNT_POST_LIKE = "CountPostLikeTopic";

    // topic：计数 - 帖子点赞数落库
    String TOPIC_COUNT_POST_LIKE_2_DB = "CountPostLike2DBTTopic";

    // Topic: 计数 - 帖子收藏数
    String TOPIC_COUNT_POST_COLLECT = "CountPostCollectTopic";

    // Topic: 计数 - 帖子收藏数落库
    String TOPIC_COUNT_POST_COLLECT_2_DB = "CountPostCollect2DBTTopic";

    // Topic: 帖子操作（发布、删除）
    String TOPIC_POST_OPERATE = "PostOperateTopic";

    // Tag 标签：帖子发布
    String TAG_POST_PUBLISH = "publishPost";

    // Tag 标签：帖子删除
    String TAG_POST_DELETE = "deletePost";

}