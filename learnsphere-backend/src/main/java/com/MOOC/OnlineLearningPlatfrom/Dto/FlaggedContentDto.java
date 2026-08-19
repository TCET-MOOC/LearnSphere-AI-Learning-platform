package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.FlaggedContent;

import java.time.LocalDateTime;

/** Flagged item with a preview of the underlying content, for the moderation queue UI. */
public class FlaggedContentDto {
    private Long id;
    private String contentType;
    private String contentTypeLabel;
    private Long contentId;
    private String reason;
    private String severity;
    private String status;
    private Long authorId;
    private String authorName;
    private String authorInitials;
    private String courseTitle;
    private String snippet;
    private LocalDateTime createdAt;

    public static FlaggedContentDto build(FlaggedContent flag, String authorName, String courseTitle, String snippet) {
        FlaggedContentDto dto = new FlaggedContentDto();
        dto.id = flag.getId();
        dto.contentType = flag.getContentType() != null ? flag.getContentType().name() : null;
        dto.contentTypeLabel = flag.getContentType() == FlaggedContent.ContentType.MESSAGE ? "Direct message" : "Comment";
        dto.contentId = flag.getContentId();
        dto.reason = flag.getReason() != null ? flag.getReason().name() : null;
        dto.severity = (flag.getReason() == FlaggedContent.Reason.BULLYING || flag.getReason() == FlaggedContent.Reason.HIGH_RISK)
                ? "high" : "medium";
        dto.status = flag.getStatus() != null ? flag.getStatus().name() : null;
        dto.authorName = authorName;
        dto.authorInitials = initials(authorName);
        dto.courseTitle = courseTitle;
        dto.snippet = snippet;
        dto.createdAt = flag.getCreatedAt();
        return dto;
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
            if (sb.length() >= 2) break;
        }
        return sb.toString();
    }

    public Long getId() { return id; }
    public String getContentType() { return contentType; }
    public String getContentTypeLabel() { return contentTypeLabel; }
    public Long getContentId() { return contentId; }
    public String getReason() { return reason; }
    public String getSeverity() { return severity; }
    public String getStatus() { return status; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public String getAuthorInitials() { return authorInitials; }
    public String getCourseTitle() { return courseTitle; }
    public String getSnippet() { return snippet; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
