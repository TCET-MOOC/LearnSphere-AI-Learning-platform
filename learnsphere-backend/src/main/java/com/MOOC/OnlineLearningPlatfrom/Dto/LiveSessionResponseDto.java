package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.LiveSession;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LiveSessionResponseDto {
    private Long id;
    private Long courseId;
    private String courseTitle;
    private String title;
    private LocalDateTime scheduledAt;
    private String status;
    private String joinUrl;
    private String sourceType;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String providerVideoId;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String sourceUrl;
    private String playbackMode;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private PlaybackDescriptor playbackDescriptor;

    public static class PlaybackDescriptor {
        private String sourceType;
        private String playbackUrl;
        private String providerVideoId;
        private LocalDateTime startedAt;

        public PlaybackDescriptor() {}

        public PlaybackDescriptor(String sourceType, String playbackUrl, String providerVideoId, LocalDateTime startedAt) {
            this.sourceType = sourceType;
            this.playbackUrl = playbackUrl;
            this.providerVideoId = providerVideoId;
            this.startedAt = startedAt;
        }

        public String getSourceType() { return sourceType; }
        public String getPlaybackUrl() { return playbackUrl; }
        public String getProviderVideoId() { return providerVideoId; }
        public LocalDateTime getStartedAt() { return startedAt; }
    }

    /**
     * Student view of one session (W5-10): the stream is reachable only through the playback
     * descriptor while LIVE; the raw source fields are never sent to students.
     */
    public static LiveSessionResponseDto forStudent(LiveSession session) {
        LiveSessionResponseDto dto = from(session);
        dto.sourceUrl = null;
        dto.providerVideoId = null;
        return dto;
    }

    /** Student list view: no stream at all, not even a descriptor (api-catalogue "List only"). */
    public static LiveSessionResponseDto forStudentList(LiveSession session) {
        LiveSessionResponseDto dto = forStudent(session);
        dto.playbackDescriptor = null;
        return dto;
    }

    /** Full view for course staff and admins, including the configured source. */
    public static LiveSessionResponseDto from(LiveSession session) {
        LiveSessionResponseDto dto = new LiveSessionResponseDto();
        dto.id = session.getId();
        if (session.getCourse() != null) {
            dto.courseId = session.getCourse().getId();
            dto.courseTitle = session.getCourse().getTitle();
        }
        dto.title = session.getTitle();
        dto.scheduledAt = session.getScheduledAt();
        dto.status = session.getStatus() != null ? session.getStatus().name() : null;
        dto.joinUrl = session.getJoinUrl();
        dto.sourceType = session.getSourceType() != null ? session.getSourceType().name() : null;
        dto.providerVideoId = session.getProviderVideoId();
        dto.sourceUrl = session.getSourceUrl();
        dto.playbackMode = session.getPlaybackMode() != null ? session.getPlaybackMode().name() : null;
        dto.startedAt = session.getStartedAt();
        dto.endedAt = session.getEndedAt();

        if (session.getStatus() == LiveSession.Status.LIVE) {
            String url = session.getSourceUrl();
            if (url == null && session.getProviderVideoId() != null) {
                url = "https://www.youtube.com/embed/" + session.getProviderVideoId();
            }
            dto.playbackDescriptor = new PlaybackDescriptor(
                    dto.sourceType,
                    url,
                    session.getProviderVideoId(),
                    session.getStartedAt()
            );
        } else {
            dto.playbackDescriptor = null;
        }

        return dto;
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public String getCourseTitle() { return courseTitle; }
    public String getTitle() { return title; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public String getStatus() { return status; }
    public String getJoinUrl() { return joinUrl; }
    public String getSourceType() { return sourceType; }
    public String getProviderVideoId() { return providerVideoId; }
    public String getSourceUrl() { return sourceUrl; }
    public String getPlaybackMode() { return playbackMode; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public PlaybackDescriptor getPlaybackDescriptor() { return playbackDescriptor; }
}
