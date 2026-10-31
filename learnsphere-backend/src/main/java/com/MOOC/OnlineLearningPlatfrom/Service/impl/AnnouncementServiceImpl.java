package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.AnnouncementRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.AnnouncementResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Announcement;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.AnnouncementRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import com.MOOC.OnlineLearningPlatfrom.Service.AnnouncementService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final OwnershipService ownershipService;

    public AnnouncementServiceImpl(AnnouncementRepository announcementRepository,
                                   CourseRepository courseRepository,
                                   EnrollmentRepository enrollmentRepository,
                                   OwnershipService ownershipService) {
        this.announcementRepository = announcementRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.ownershipService = ownershipService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementResponseDto> getAnnouncements(CustomUserDetails principal) {
        return getAnnouncements(principal, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementResponseDto> getAnnouncements(CustomUserDetails principal, Long courseId) {
        if (principal == null || principal.getUser() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        LocalDateTime now = LocalDateTime.now();
        List<String> roles = principal.getRoles();
        boolean isAdmin = roles.contains("ADMIN");
        boolean isTeacher = roles.contains("TEACHER");

        if (courseId != null) {
            ownershipService.requireEnrolled(courseId, principal);
            return announcementRepository.findActiveByCourseId(courseId, now).stream()
                    .map(AnnouncementResponseDto::from)
                    .toList();
        }

        if (isAdmin) {
            return announcementRepository.findActiveForAdmin(null, now).stream()
                    .map(AnnouncementResponseDto::from)
                    .toList();
        }

        Announcement.Audience audience = resolveAudience(principal);
        List<Announcement.Audience> audiences = audience == Announcement.Audience.ALL
                ? List.of(Announcement.Audience.ALL)
                : List.of(Announcement.Audience.ALL, audience);

        if (isTeacher) {
            Long teacherId = principal.getUser().getUserId();
            List<Long> taughtCourseIds = courseRepository.findByTeacher_UserId(teacherId).stream()
                    .map(Course::getCourseId)
                    .toList();
            if (taughtCourseIds.isEmpty()) {
                return announcementRepository.findActiveGlobal(audiences, now).stream()
                        .map(AnnouncementResponseDto::from)
                        .toList();
            }
            return announcementRepository.findActiveForTeacher(audiences, taughtCourseIds, now).stream()
                    .map(AnnouncementResponseDto::from)
                    .toList();
        }

        // Student feed: global announcements + enrolled courses announcements
        Long studentId = principal.getUser().getUserId();
        List<Long> enrolledCourseIds = enrollmentRepository.findByUserId(studentId).stream()
                .map(e -> e.getCourse() != null ? e.getCourse().getCourseId() : null)
                .filter(Objects::nonNull)
                .toList();

        if (enrolledCourseIds.isEmpty()) {
            return announcementRepository.findActiveGlobal(audiences, now).stream()
                    .map(AnnouncementResponseDto::from)
                    .toList();
        }

        return announcementRepository.findActiveForStudent(audiences, enrolledCourseIds, now).stream()
                .map(AnnouncementResponseDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementResponseDto> getCourseAnnouncements(Long courseId, CustomUserDetails principal) {
        ownershipService.requireEnrolled(courseId, principal);
        LocalDateTime now = LocalDateTime.now();
        return announcementRepository.findActiveByCourseId(courseId, now).stream()
                .map(AnnouncementResponseDto::from)
                .toList();
    }

    @Override
    public AnnouncementResponseDto createAnnouncement(AnnouncementRequestDto request, CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("title is required");
        }

        List<String> roles = principal.getRoles();
        boolean isAdmin = roles.contains("ADMIN");
        boolean isTeacher = roles.contains("TEACHER");

        if (!isAdmin && !isTeacher) {
            throw new AccessDeniedException("Only teachers or admins can create announcements");
        }

        Course course = null;
        if (!isAdmin) {
            // Teacher announcements must be course-scoped (Rule A-17, [E-F13])
            if (request.getCourseId() == null) {
                throw new BadRequestException("Teachers can only create course-scoped announcements");
            }
            ownershipService.requireCourseStaff(request.getCourseId(), principal);
            course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));
        } else {
            if (request.getCourseId() != null) {
                course = courseRepository.findById(request.getCourseId())
                        .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));
            }
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setBody(request.getBody());
        announcement.setCategory(request.getCategory());
        announcement.setAuthor(principal.getUser());
        announcement.setCourse(course);
        announcement.setPinned(Boolean.TRUE.equals(request.getPinned()));
        announcement.setAudience(parseAudience(request.getAudience()));
        announcement.setExpiresAt(request.getExpiresAt());

        return AnnouncementResponseDto.from(announcementRepository.save(announcement));
    }

    @Override
    public AnnouncementResponseDto updateAnnouncement(Long id, AnnouncementRequestDto request, CustomUserDetails principal) {
        Announcement announcement = findOrThrow(id);
        requireAuthorOrStaff(announcement, principal);

        if (request.getTitle() != null && !request.getTitle().isBlank()) announcement.setTitle(request.getTitle());
        if (request.getBody() != null) announcement.setBody(request.getBody());
        if (request.getCategory() != null) announcement.setCategory(request.getCategory());
        if (request.getAudience() != null) announcement.setAudience(parseAudience(request.getAudience()));
        if (request.getPinned() != null) announcement.setPinned(request.getPinned());
        if (request.getExpiresAt() != null) announcement.setExpiresAt(request.getExpiresAt());

        return AnnouncementResponseDto.from(announcementRepository.save(announcement));
    }

    @Override
    public void deleteAnnouncement(Long id, CustomUserDetails principal) {
        Announcement announcement = findOrThrow(id);
        requireAuthorOrStaff(announcement, principal);
        announcementRepository.delete(announcement);
    }

    @Override
    public AnnouncementResponseDto togglePin(Long id, CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        Announcement announcement = findOrThrow(id);
        boolean isAdmin = principal.getRoles().contains("ADMIN");
        boolean isTeacher = principal.getRoles().contains("TEACHER");

        if (!isAdmin) {
            if (!isTeacher || announcement.getCourse() == null) {
                throw new AccessDeniedException("Only admins can pin global announcements");
            }
            ownershipService.requireCourseStaff(announcement.getCourse().getCourseId(), principal);
        }

        announcement.setPinned(!Boolean.TRUE.equals(announcement.getPinned()));
        return AnnouncementResponseDto.from(announcementRepository.save(announcement));
    }

    private Announcement findOrThrow(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));
    }

    private void requireAuthorOrStaff(Announcement announcement, CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        boolean isAdmin = principal.getRoles().contains("ADMIN");
        if (isAdmin) return;

        boolean isAuthor = announcement.getAuthor() != null
                && announcement.getAuthor().getUserId().equals(principal.getUser().getUserId());

        if (announcement.getCourse() != null) {
            try {
                ownershipService.requireCourseStaff(announcement.getCourse().getCourseId(), principal);
            } catch (Exception e) {
                if (!isAuthor) throw new AccessDeniedException("Only the author or course staff can modify this announcement");
            }
        } else if (!isAuthor) {
            throw new AccessDeniedException("Only the author or an admin can modify this announcement");
        }
    }

    private Announcement.Audience resolveAudience(CustomUserDetails principal) {
        List<String> roles = principal.getRoles();
        if (roles.contains("ADMIN")) return Announcement.Audience.ADMIN;
        if (roles.contains("TEACHER")) return Announcement.Audience.TEACHER;
        if (roles.contains("STUDENT")) return Announcement.Audience.STUDENT;
        return Announcement.Audience.ALL;
    }

    private Announcement.Audience parseAudience(String value) {
        if (value == null || value.isBlank()) return Announcement.Audience.ALL;
        try {
            return Announcement.Audience.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid audience: " + value + ". Must be one of "
                    + Arrays.toString(Announcement.Audience.values()));
        }
    }
}
