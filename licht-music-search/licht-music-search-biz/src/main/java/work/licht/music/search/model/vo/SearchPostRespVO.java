package work.licht.music.search.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 搜索帖子
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchPostRespVO {

    // 帖子ID
    private Long postId;

    // 封面
    private String cover;

    // 标题
    private String title;

    // 发布者头像
    private String avatar;

    // 发布者昵称
    private String nickname;

    // 最后一次编辑时间
    private String updateTime;

    // 被点赞总数
    private String likeTotal;

    // 被评论数
    private String commentTotal;

    // 被收藏数
    private String collectTotal;

}

