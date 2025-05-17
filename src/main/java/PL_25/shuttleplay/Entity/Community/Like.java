package PL_25.shuttleplay.Entity.Community;

import jakarta.persistence.Id;

public class Like {
    @Id
    public Long likeId;

    // @Joincol 해야됨
    private String userId;  // 좋아요 누른 사람
    // @joincolumn
    private String postId;  // 어떤 게시글에 누른건지
}
