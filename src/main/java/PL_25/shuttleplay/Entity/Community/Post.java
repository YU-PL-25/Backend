package PL_25.shuttleplay.Entity.Community;

import PL_25.shuttleplay.Entity.Community.Comment;
import PL_25.shuttleplay.Entity.Community.Like;
import PL_25.shuttleplay.Entity.User.User;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class Post {
    @Id
    public Long postId;

    private String title;
    private String category;
    private String content;
    private User user;  // 작성자
    private LocalDate createdAt;
    private List<Comment> comments;
    private List<Like> likes;

}
