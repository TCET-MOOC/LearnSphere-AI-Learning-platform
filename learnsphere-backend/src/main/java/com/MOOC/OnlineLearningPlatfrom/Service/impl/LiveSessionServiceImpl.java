package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionResponseDto;
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

import java.util.ArrayList;
import java.util.List;
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
    public LiveSessionResponseDto start(Long id, CustomUserDetails principal) {
        LiveSession session = liveSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Live session not found with id: " + id));
        if (session.getCourse() == null) {
            throw new ResourceNotFoundException("Live session not found with id: " + id);
        }
        ownershipService.requireCourseStaff(session.getCourse().getId(), principal);
        session.setStatus(LiveSession.Status.LIVE);
        session.setJoinUrl("/live/" + UUID.randomUUID());
        LiveSessionResponseDto dto = LiveSessionResponseDto.from(liveSessionRepository.save(session));
        messagingTemplate.convertAndSend("/topic/live-sessions/" + id, dto);
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
        LiveSessionResponseDto dto = LiveSessionResponseDto.from(liveSessionRepository.save(session));
        messagingTemplate.convertAndSend("/topic/live-sessions/" + id, dto);
        return dto;
    }
}
