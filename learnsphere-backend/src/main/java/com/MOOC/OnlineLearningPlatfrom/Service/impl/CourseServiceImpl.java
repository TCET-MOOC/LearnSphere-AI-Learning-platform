package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.CourseResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.LectureResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.LectureRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.CourseService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository departmentRepository;
    private final JdbcTemplate jdbcTemplate;
    private final com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService ownershipService;
    private final com.MOOC.OnlineLearningPlatfrom.Service.PublicCacheService publicCacheService;

    @org.springframework.beans.factory.annotation.Autowired
    public CourseServiceImpl(CourseRepository courseRepository,
                             LectureRepository lectureRepository,
                             com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository departmentRepository,
                             JdbcTemplate jdbcTemplate,
                             com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService ownershipService,
                             @org.springframework.beans.factory.annotation.Autowired(required = false)
                             com.MOOC.OnlineLearningPlatfrom.Service.PublicCacheService publicCacheService) {
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.departmentRepository = departmentRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.ownershipService = ownershipService;
        this.publicCacheService = publicCacheService;
    }

    public CourseServiceImpl(CourseRepository courseRepository,
                             LectureRepository lectureRepository,
                             com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository departmentRepository,
                             JdbcTemplate jdbcTemplate,
                             com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService ownershipService) {
        this(courseRepository, lectureRepository, departmentRepository, jdbcTemplate, ownershipService, null);
    }

    public CourseServiceImpl(CourseRepository courseRepository, LectureRepository lectureRepository, JdbcTemplate jdbcTemplate) {
        this(courseRepository, lectureRepository, null, jdbcTemplate, null, null);
    }

    @Override
    public List<CourseResponseDto> getCourses(String status, String department, Long teacherId) {
        return getCourses(status, department, teacherId, null);
    }

    private boolean isCurrentCallerAdmin() {
        org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getRoles() != null && 
                    (cud.getRoles().contains("ADMIN") || cud.getRoles().contains("ROLE_ADMIN"));
        }
        return false;
    }

    @Override
    public List<CourseResponseDto> getCourses(String status, String department, Long teacherId, String search) {
        List<Course> courses;
        boolean isAdmin = isCurrentCallerAdmin();

        if (teacherId != null) {
            courses = courseRepository.findByTeacher_UserId(teacherId);
        } else if (status != null && department != null) {
            Long deptId = tryParseLong(department);
            Course.Status parsedStatus;
            try {
                parsedStatus = Course.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return List.of();
            }
            if (parsedStatus != Course.Status.LIVE && !isAdmin) {
                return List.of();
            }
            if (deptId != null) {
                courses = courseRepository.findByStatusAndDepartment_DepartmentId(parsedStatus, deptId);
            } else {
                courses = courseRepository.findByStatusAndDepartmentLabel(parsedStatus, department);
            }
        } else if (status != null) {
            Course.Status parsedStatus;
            try {
                parsedStatus = Course.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return List.of();
            }
            if (parsedStatus != Course.Status.LIVE && !isAdmin) {
                return List.of();
            }
            courses = courseRepository.findByStatus(parsedStatus);
        } else if (department != null) {
            Long deptId = tryParseLong(department);
            if (deptId != null) {
                courses = courseRepository.findByStatusAndDepartment_DepartmentId(Course.Status.LIVE, deptId);
            } else {
                courses = courseRepository.findByStatusAndDepartmentLabel(Course.Status.LIVE, department);
            }
        } else {
            courses = courseRepository.findByStatus(Course.Status.LIVE);
        }
        if (search != null && !search.isBlank()) {
            String[] tokens = search.toLowerCase().trim().split("\\s+");
            courses = courses.stream()
                    .filter(c -> {
                        String haystack = ((c.getTitle() != null ? c.getTitle() : "") + " "
                                + (c.getDescription() != null ? c.getDescription() : "")).toLowerCase();
                        for (String token : tokens) {
                            if (!haystack.contains(token)) return false;
                        }
                        return true;
                    })
                    .toList();
        }
        return courses.stream()
                .map(c -> CourseResponseDto.from(c, lectureRepository.countByCourse_Id(c.getId())))
                .toList();
    }

    private Long tryParseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public CourseResponseDto getCourseById(Long id) {
        Course course = getCourseEntity(id);
        if (course.getStatus() != Course.Status.LIVE) {
            boolean isStaffOrAdmin = false;
            org.springframework.security.core.Authentication auth = 
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
                boolean isAdmin = cud.getRoles() != null && 
                        (cud.getRoles().contains("ADMIN") || cud.getRoles().contains("ROLE_ADMIN"));
                boolean isOwner = course.getTeacher() != null && cud.getUser() != null &&
                        course.getTeacher().getUserId() != null && course.getTeacher().getUserId().equals(cud.getUser().getUserId());
                isStaffOrAdmin = isAdmin || isOwner;
            }
            if (!isStaffOrAdmin) {
                throw new ResourceNotFoundException("Course not found with id: " + id);
            }
        }
        return CourseResponseDto.from(course, lectureRepository.countByCourse_Id(id));
    }

    @Override
    public Course getCourseEntity(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    private com.MOOC.OnlineLearningPlatfrom.Entity.Department resolveDepartment(Course course) {
        if (course == null || departmentRepository == null) return null;
        if (course.getDepartment() != null && course.getDepartment().getDepartmentId() != null) {
            return departmentRepository.findById(course.getDepartment().getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + course.getDepartment().getDepartmentId()));
        }
        if (course.getDepartmentId() != null) {
            return departmentRepository.findById(course.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + course.getDepartmentId()));
        }
        if (course.getDepartmentLabel() != null && !course.getDepartmentLabel().isBlank()) {
            return departmentRepository.findByNameIgnoreCase(course.getDepartmentLabel().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with name: " + course.getDepartmentLabel()));
        }
        return null;
    }

    @Override
    public CourseResponseDto createCourse(Course course, CustomUserDetails principal) {
        course.setTeacher(principal.getUser());
        course.setStatus(Course.Status.DRAFT);

        com.MOOC.OnlineLearningPlatfrom.Entity.Department dept = resolveDepartment(course);
        if (dept == null) {
            throw new com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException("Department is required for course creation");
        }
        course.setDepartment(dept);
        course.setDepartmentLabel(dept.getName());

        Course saved = courseRepository.save(course);
        if (publicCacheService != null) {
            publicCacheService.invalidateAllCourses();
        }
        return CourseResponseDto.from(saved, 0);
    }

    @Override
    public CourseResponseDto updateCourse(Long id, Course updates, CustomUserDetails principal) {
        Course course = getCourseEntity(id);
        assertOwner(course, principal);

        if (updates.getTitle() != null) course.setTitle(updates.getTitle());
        if (updates.getDescription() != null) course.setDescription(updates.getDescription());
        
        com.MOOC.OnlineLearningPlatfrom.Entity.Department dept = resolveDepartment(updates);
        if (dept != null) {
            course.setDepartment(dept);
            course.setDepartmentLabel(dept.getName());
        }

        if (updates.getThumbnail() != null) course.setThumbnail(updates.getThumbnail());
        if (updates.getPrice() != null) course.setPrice(updates.getPrice());
        if (updates.getStatus() != null) {
            boolean isAdmin = principal != null && principal.getRoles() != null &&
                    (principal.getRoles().contains("ADMIN") || principal.getRoles().contains("ROLE_ADMIN"));
            if (updates.getStatus() == Course.Status.LIVE && !isAdmin) {
                throw new org.springframework.security.access.AccessDeniedException("Course publication requires administrator approval");
            }
            course.setStatus(updates.getStatus());
        }

        Course saved = courseRepository.save(course);
        if (publicCacheService != null) {
            publicCacheService.invalidateCourse(id);
        }
        return CourseResponseDto.from(saved, lectureRepository.countByCourse_Id(id));
    }

    @Override
    @Transactional
    public void deleteCourse(Long id, CustomUserDetails principal) {
        Course course = getCourseEntity(id);
        assertOwner(course, principal);

        course.setStatus(Course.Status.ARCHIVED);
        courseRepository.save(course);
        if (publicCacheService != null) {
            publicCacheService.invalidateCourse(id);
        }
    }

    @Override
    @Transactional
    public CourseResponseDto restoreCourse(Long id, CustomUserDetails principal) {
        Course course = getCourseEntity(id);
        assertOwner(course, principal);

        if (course.getStatus() != Course.Status.ARCHIVED) {
            throw new com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException("Course is not archived");
        }

        course.setStatus(Course.Status.DRAFT);
        Course saved = courseRepository.save(course);
        if (publicCacheService != null) {
            publicCacheService.invalidateCourse(id);
        }
        return CourseResponseDto.from(saved, lectureRepository.countByCourse_Id(id));
    }

    @Override
    public List<LectureResponseDto> getLectures(Long courseId) {
        // ensure course exists
        getCourseEntity(courseId);
        return lectureRepository.findByCourse_IdOrderByNumberAsc(courseId).stream()
                .map(LectureResponseDto::from)
                .toList();
    }

    @Override
    public LectureResponseDto getLectureById(Long lectureId) {
        return LectureResponseDto.from(getLectureEntity(lectureId));
    }

    @Override
    public Lecture getLectureEntity(Long lectureId) {
        return lectureRepository.findById(lectureId)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found with id: " + lectureId));
    }

    @Override
    public LectureResponseDto addLecture(Long courseId, Lecture lecture, CustomUserDetails principal) {
        throw new BadRequestException("Legacy lecture authoring is retired (A-05). Please author curriculum via /api/courses/{id}/modules and /api/modules/{id}/contents.");
    }

    @Override
    public List<LectureResponseDto> addLecturesBulk(Long courseId, List<Lecture> lectures, CustomUserDetails principal) {
        throw new BadRequestException("Legacy bulk lecture authoring is retired (A-05). Please author curriculum via /api/courses/{id}/modules and /api/modules/{id}/contents.");
    }

    @Override
    public LectureResponseDto updateLecture(Long lectureId, Lecture updates, CustomUserDetails principal) {
        throw new BadRequestException("Legacy lecture modification is retired (A-05). Please edit curriculum via /api/contents/{id}.");
    }

    @Override
    public void deleteLecture(Long lectureId, CustomUserDetails principal) {
        throw new BadRequestException("Legacy lecture deletion is retired (A-05). Please delete curriculum via /api/contents/{id}.");
    }

    private void assertOwner(Course course, CustomUserDetails principal) {
        if (ownershipService != null) {
            ownershipService.requireCourseStaff(course.getId(), principal);
            return;
        }
        if (principal == null || principal.getUser() == null) {
            return;
        }
        boolean isAdmin = principal.getRoles() != null && 
                (principal.getRoles().contains("ADMIN") || principal.getRoles().contains("ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }
        if (course.getTeacher() != null && course.getTeacher().getUserId() != null && principal.getUser().getUserId() != null) {
            if (course.getTeacher().getUserId().equals(principal.getUser().getUserId())) {
                return;
            }
        }
        if (course.getTeacher() != null && course.getTeacher().getEmail() != null && principal.getUsername() != null) {
            if (course.getTeacher().getEmail().equalsIgnoreCase(principal.getUsername())) {
                return;
            }
        }
        throw new ResourceNotFoundException("Course not found with id: " + course.getId());
    }
}
