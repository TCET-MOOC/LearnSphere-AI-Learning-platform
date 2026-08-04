package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.DiscussionPost;

import java.time.LocalDateTime;
import java.util.List;

public class DiscussionPostDto {
    private Long id;
    private Long courseId;
    private Long lectureId;
    private Long authorId;
    private String authorName;
    private String authorAvatarUrl;
    private String body;
    private Long parentPostId;
    private LocalDateTime createdAt;
    private Boolean flagged;
    private List<DiscussionPostDto> replies;

    public static DiscussionPostDto from(DiscussionPost post) {
        return from(post, List.of());
    }

    public static DiscussionPostDto from(DiscussionPost post, List<DiscussionPostDto> replies) {
        DiscussionPostDto dto = new DiscussionPostDto();
        dto.id = post.getId();
        dto.courseId = post.getCourse() != null ? post.getCourse().getId() : null;
        dto.lectureId = post.getLecture() != null ? post.getLecture().getId() : null;
        if (post.getAuthor() != null) {
            dto.authorId = post.getAuthor().getUserId();
            dto.authorName = post.getAuthor().getFullName();
            dto.authorAvatarUrl = post.getAuthor().getAvatarUrl();
        }
        dto.body = post.getBody();
        dto.parentPostId = post.getParentPost() != null ? post.getParentPost().getId() : null;
        dto.createdAt = post.getCreatedAt();
        dto.flagged = post.getFlagged();
        dto.replies = replies;
        return dto;
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public Long getLectureId() { return lectureId; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public String getBody() { return body; }
    public Long getParentPostId() { return parentPostId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Boolean getFlagged() { return flagged; }
    public List<DiscussionPostDto> getReplies() { return replies; }
}
