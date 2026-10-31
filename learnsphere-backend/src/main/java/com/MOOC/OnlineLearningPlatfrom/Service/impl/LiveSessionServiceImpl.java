package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionSourceDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.LiveSession;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.LiveSessionRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import com.MOOC.OnlineLearningPlatfrom.Service.LiveSessionService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class LiveSessionServiceImpl implements LiveSessionService {

    private final LiveSessionRepository liveSessionRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final OwnershipService ownershipService;

    public LiveSessionServiceImpl(LiveSessionRepository liveSessionRepository, 
                                  CourseRepository courseRepository,
                                  EnrollmentRepository enrollmentRepository,
                                  SimpMessagingTemplate messagingTemplate,
                                  OwnershipService ownershipService) {
        this.liveSessionRepository = liveSessionRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.messagingTemplate = messagingTemplate;
        this.ownershipService = ownershipService;
    }

    @Override
    public List<LiveSessionResponseDto> getMySessions(CustomUserDetails principal) {
        return liveSessionRepository.findByTeacher_UserIdOrderByScheduledAtDesc(principal.getUser().getUserId()).stream()
                .map(LiveSessionResponseDto::from)
                .toList();
    }

    @Override
    public List<LiveSessionResponseDto> getStudentSessions(CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            return List.of();
        }
        if (ownershipService.isAdmin(principal)) {
            return liveSessionRepository.findAll().stream()
                    .sorted((a, b) -> {
                        if (a.getScheduledAt() == null) return 1;
                        if (b.getScheduledAt() == null) return -1;
                        return b.getScheduledAt().compareTo(a.getScheduledAt());
                    })
                    .map(LiveSessionResponseDto::from)
                    .toList();
        }

        Long userId = principal.getUser().getUserId();
        List<com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment> enrollments = enrollmentRepository.findByUserId(userId);
        List<Long> enrolledCourseIds = new ArrayList<>(enrollments.stream()
                .map(e -> e.getCourse() != null ? e.getCourse().getId() : null)
                .filter(Objects::nonNull)
                .toList());

        List<Course> taughtCourses = courseRepository.findByTeacher_UserId(userId);
        if (taughtCourses != null) {
            for (Course c : taughtCourses) {
                if (!enrolledCourseIds.contains(c.getId())) {
                    enrolledCourseIds.add(c.getId());
                }
            }
        }

        if (enrolledCourseIds.isEmpty()) {
            return List.of();
        }

        return liveSessionRepository.findAll().stream()
                .filter(s -> s.getCourse() != null && enrolledCourseIds.contains(s.getCourse().getId()))
                .sorted((a, b) -> {
                    if (a.getScheduledAt() == null) return 1;
                    if (b.getScheduledAt() == null) return -1;
                    return b.getScheduledAt().compareTo(a.getScheduledAt());
                })
                .map(LiveSessionResponseDto::from)
                .toList();
    }

    @Override
    public LiveSessionResponseDto getSessionById(Long id, CustomUserDetails principal) {
        LiveSession session = liveSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Live session not found with id: " + id));
        if (session.getCourse() != null) {
            ownershipService.requireEnrolled(session.getCourse().getId(), principal);
        } else if (!ownershipService.isAdmin(principal)) {
            throw new ResourceNotFoundException("Live session not found with id: " + id);
        }
        return LiveSessionResponseDto.from(session);
    }

    @Override
    public LiveSessionResponseDto getSessionById(Long id) {
        LiveSession session = liveSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Live session not found with id: " + id));
        return LiveSessionResponseDto.from(session);
    }

    @Override
    public LiveSessionResponseDto create(LiveSessionRequestDto request, CustomUserDetails principal) {
        if (request.getCourseId() == null) {
            throw new BadRequestException("courseId is required.");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("A session title is required.");
        }
        if (request.getScheduledAt() == null) {
            throw new BadRequestException("scheduledAt is required.");
        }
        Course course = ownershipService.requireCourseStaff(request.getCourseId(), principal);

        LiveSession session = new LiveSession();
        session.setCourse(course);
        session.setTeacher(principal.getUser());
        session.setTitle(request.getTitle());
        session.setScheduledAt(request.getScheduledAt());
        session.setStatus(LiveSession.Status.SCHEDULED);
        return LiveSessionResponseDto.from(liveSessionRepository.save(session));
    }

    @Override
    public LiveSessionResponseDto setSource(Long id, LiveSessionSourceDto dto, CustomUserDetails principal) {
        LiveSession session = liveSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Live session not found with id: " + id));
        if (session.getCourse() == null) {
            throw new ResourceNotFoundException("Live session not found with id: " + id);
        }
        ownershipService.requireCourseStaff(session.getCourse().getId(), principal);

        if (dto == null || dto.getSourceType() == null) {
            throw new BadRequestException("sourceType is required.");
        }

        String sourceUrl = dto.getSourceUrl();
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            String lower = sourceUrl.toLowerCase();
            if (lower.startsWith("rtmp://") || lower.startsWith("rtmps://")) {
                throw new BadRequestException("RTMP protocol is not supported for playback. Use HLS or YouTube Live.");
            }
            if (lower.contains("key=") || lower.contains("stream_key") || lower.contains("secret") || lower.contains("streamkey")) {
                throw new BadRequestException("Encoder stream keys must not be exposed in public playback source URLs.");
            }
        }

        String providerVideoId = dto.getProviderVideoId();
        if (providerVideoId != null && !providerVideoId.isBlank()) {
            String lower = providerVideoId.toLowerCase();
            if (lower.contains("stream_key") || lower.contains("rtmp") || lower.contains("secret")) {
                throw new BadRequestException("Invalid video id or encoder key detected.");
            }
        }

        if (dto.getSourceType() == LiveSession.SourceType.YOUTUBE_LIVE) {
            if (providerVideoId == null || providerVideoId.isBlank()) {
                throw new BadRequestException("providerVideoId is required for YOUTUBE_LIVE.");
            }
            session.setSourceType(LiveSession.SourceType.YOUTUBE_LIVE);
            session.setProviderVideoId(providerVideoId.trim());
            session.setPlaybackMode(LiveSession.PlaybackMode.YOUTUBE);
            if (sourceUrl != null && !sourceUrl.isBlank()) {
                session.setSourceUrl(sourceUrl.trim());
            } else {
                session.setSourceUrl("https://www.youtube.com/embed/" + providerVideoId.trim());
            }
        } else if (dto.getSourceType() == LiveSession.SourceType.HLS) {
            if (sourceUrl == null || sourceUrl.isBlank()) {
                throw new BadRequestException("sourceUrl is required for HLS.");
            }
            if (!sourceUrl.toLowerCase().startsWith("https://")) {
                throw new BadRequestException("HLS sourceUrl must use secure HTTPS.");
            }
            session.setSourceType(LiveSession.SourceType.HLS);
            session.setSourceUrl(sourceUrl.trim());
            session.setPlaybackMode(LiveSession.PlaybackMode.HLS);
            if (providerVideoId != null && !providerVideoId.isBlank()) {
                session.setProviderVideoId(providerVideoId.trim());
            }
        } else {
            throw new BadRequestException("Unsupported source type: " + dto.getSourceType());
        }

        return LiveSessionResponseDto.from(liveSessionRepository.save(session));
    }

    @Override
    public LiveSessionResponseDto start(Long id, CustomUserDetails principal) {
        LiveSession session = liveSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Live session not found with id: " + id));
        if (session.getCourse() == null) {
            throw new ResourceNotFoundException("Live session not found with id: " + id);
        }
        ownershipService.requireCourseStaff(session.getCourse().getId(), principal);

        if (session.getSourceType() == null ||
                (session.getSourceType() == LiveSession.SourceType.YOUTUBE_LIVE && (session.getProviderVideoId() == null || session.getProviderVideoId().isBlank())) ||
                (session.getSourceType() == LiveSession.SourceType.HLS && (session.getSourceUrl() == null || session.getSourceUrl().isBlank()))) {
            throw new BadRequestException("Cannot start live session without a validated playback source.");
        }

        session.setStatus(LiveSession.Status.LIVE);
        session.setStartedAt(LocalDateTime.now());
        session.setJoinUrl("/live/" + UUID.randomUUID());
        LiveSessionResponseDto dto = LiveSessionResponseDto.from(liveSessionRepository.save(session));
        messagingTemplate.convertAndSend("/topic/live-sessions/" + id, dto);
        messagingTemplate.convertAndSend("/topic/live-sessions/" + id + "/status", Map.of("status", "LIVE", "sessionId", id));
        return dto;
    }

    @Override
    public LiveSessionResponseDto end(Long id, CustomUserDetails principal) {
        LiveSession session = liveSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Live session not found with id: " + id));
        if (session.getCourse() == null) {
            throw new ResourceNotFoundException("Live session not found with id: " + id);
        }
        ownershipService.requireCourseStaff(session.getCourse().getId(), principal);
        session.setStatus(LiveSession.Status.ENDED);
        session.setEndedAt(LocalDateTime.now());
        LiveSessionResponseDto dto = LiveSessionResponseDto.from(liveSessionRepository.save(session));
        messagingTemplate.convertAndSend("/topic/live-sessions/" + id, dto);
        messagingTemplate.convertAndSend("/topic/live-sessions/" + id + "/status", Map.of("status", "ENDED", "sessionId", id));
        return dto;
    }
}
