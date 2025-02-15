package work.licht.music.search.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 搜索用户
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchUserRespVO {

    // 用户ID
    private Long userId;

    // 用户名
    private String username;

    // 昵称
    private String nickname;

    // 头像
    private String avatar;

    // 帖子发布总数
    private String postTotal;

    // 粉丝总数
    private String followerTotal;

}