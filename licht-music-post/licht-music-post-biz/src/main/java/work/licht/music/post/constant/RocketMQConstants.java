package work.licht.music.post.constant;

public interface RocketMQConstants {

    // Topic 主题：删除帖子本地缓存
    String TOPIC_DELETE_POST_LOCAL_CACHE = "DeletePostLocalCacheTopic";

    // Topic: 点赞、取消点赞共用一个
    String TOPIC_LIKE_OR_UNLIKE = "LikeUnlikeTopic";

    // 点赞标签
    String TAG_LIKE = "Like";

    // Tag 标签：取消点赞
    String TAG_UNLIKE = "Unlike";


    // Topic: 计数 - 帖子点赞数
    String TOPIC_COUNT_POST_LIKE = "CountPostLikeTopic";

    // Topic: 收藏、取消收藏共用一个
    String TOPIC_COLLECT_OR_UN_COLLECT = "CollectUnCollectTopic";

    // Tag 标签：收藏
    String TAG_COLLECT = "Collect";

    // Tag 标签：取消收藏
    String TAG_UN_COLLECT = "UnCollect";

    //  Topic: 计数 - 帖子收藏数
    String TOPIC_COUNT_POST_COLLECT = "CountPostCollectTopic";

    // Topic: 帖子操作（发布、删除）
    String TOPIC_POST_OPERATE = "PostOperateTopic";

    // Tag 标签：帖子发布
    String TAG_POST_PUBLISH = "publishPost";

    // Tag 标签：帖子删除
    String TAG_POST_DELETE = "deletePost";

}
