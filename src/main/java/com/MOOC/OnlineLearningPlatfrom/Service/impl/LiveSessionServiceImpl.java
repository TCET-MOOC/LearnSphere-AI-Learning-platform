package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.LiveSession;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.LiveSessionRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.LiveSessionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LiveSessionServiceImpl implements LiveSessionService {

    private final LiveSessionRepository liveSessionRepository;
    private final CourseRepository courseRepository;
    private final com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository enrollmentRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public LiveSessionServiceImpl(LiveSessionRepository liveSessionRepository, 
                                  CourseRepository courseRepository,
                                  com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository enrollmentRepository,
                                  org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.liveSessionRepository = liveSessionRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public List<LiveSessionResponseDto> getMySessions(CustomUserDetails principal) {
        return liveSessionRepository.findByTeacher_UserIdOrderByScheduledAtDesc(principal.getUser().getUserId()).stream()
                .map(LiveSessionResponseDto::from)
                .toList();
    }

    @Override
    public List<LiveSessionResponseDto> getStudentSessions(CustomUserDetails principal) {
        Long userId = principal.getUser().getUserId();
        List<com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment> enrollments = enrollmentRepository.findByUserId(userId);
        List<Long> enrolledCourseIds = enrollments.stream()
                .map(e -> e.getCourse() != null ? e.getCourse().getId() : null)
                .filter(java.util.Objects::nonNull)
                .toList();

        List<LiveSession> allSessions = liveSessionRepository.findAll();
        // Return sessions for enrolled courses first, or all active/scheduled sessions
        return allSessions.stream()
                .filter(s -> enrolledCourseIds.isEmpty() || (s.getCourse() != null && enrolledCourseIds.contains(s.getCourse().getId())))
                .sorted((a, b) -> {
                    if (a.getScheduledAt() == null) return 1;
                    if (b.getScheduledAt() == null) return -1;
                    return b.getScheduledAt().compareTo(a.getScheduledAt());
                })
                .map(LiveSessionResponseDto::from)
                .toList();
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
        Course course = assertOwnedCourse(request.getCourseId(), principal.getUser().getUserId());

        LiveSession session = new LiveSession();
        session.setCourse(course);
        session.setTeacher(principal.getUser());
        session.setTitle(request.getTitle());
        session.setScheduledAt(request.getScheduledAt());
        session.setStatus(LiveSession.Status.SCHEDULED);
        return LiveSessionResponseDto.from(liveSessionRepository.save(session));
    }

    @Override
    public LiveSessionResponseDto start(Long id, CustomUserDetails principal) {
        LiveSession session = assertOwnedSession(id, principal.getUser().getUserId());
        session.setStatus(LiveSession.Status.LIVE);
        session.setJoinUrl("/live/" + UUID.randomUUID());
        LiveSessionResponseDto dto = LiveSessionResponseDto.from(liveSessionRepository.save(session));
        messagingTemplate.convertAndSend("/topic/live-sessions/" + id, dto);
        return dto;
    }

    @Override
    public LiveSessionResponseDto end(Long id, CustomUserDetails principal) {
        LiveSession session = assertOwnedSession(id, principal.getUser().getUserId());
        session.setStatus(LiveSession.Status.ENDED);
        LiveSessionResponseDto dto = LiveSessionResponseDto.from(liveSessionRepository.save(session));
        messagingTemplate.convertAndSend("/topic/live-sessions/" + id, dto);
        return dto;
    }

    private Course assertOwnedCourse(Long courseId, Long teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        if (course.getTeacher() == null || !course.getTeacher().getUserId().equals(teacherId)) {
            throw new BadRequestException("You do not own this course.");
        }
        return course;
    }

    private LiveSession assertOwnedSession(Long id, Long teacherId) {
        LiveSession session = liveSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Live session not found with id: " + id));
        if (session.getTeacher() == null || !session.getTeacher().getUserId().equals(teacherId)) {
            throw new ResourceNotFoundException("Live session not found with id: " + id);
        }
        return session;
    }
}
