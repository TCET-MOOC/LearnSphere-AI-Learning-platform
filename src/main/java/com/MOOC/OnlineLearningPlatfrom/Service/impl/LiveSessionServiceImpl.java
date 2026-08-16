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

    public LiveSessionServiceImpl(LiveSessionRepository liveSessionRepository, CourseRepository courseRepository) {
        this.liveSessionRepository = liveSessionRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public List<LiveSessionResponseDto> getMySessions(CustomUserDetails principal) {
        return liveSessionRepository.findByTeacher_UserIdOrderByScheduledAtDesc(principal.getUser().getUserId()).stream()
                .map(LiveSessionResponseDto::from)
                .toList();
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
        return LiveSessionResponseDto.from(liveSessionRepository.save(session));
    }

    @Override
    public LiveSessionResponseDto end(Long id, CustomUserDetails principal) {
        LiveSession session = assertOwnedSession(id, principal.getUser().getUserId());
        session.setStatus(LiveSession.Status.ENDED);
        return LiveSessionResponseDto.from(liveSessionRepository.save(session));
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
