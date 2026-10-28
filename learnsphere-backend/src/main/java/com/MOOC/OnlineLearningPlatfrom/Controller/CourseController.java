package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.CourseResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.LectureResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.CourseService;
import com.MOOC.OnlineLearningPlatfrom.Service.PublicCacheService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class CourseController {

    private final CourseService courseService;
    private final PublicCacheService publicCacheService;
    private final com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService ownershipService;

    public CourseController(CourseService courseService,
                            PublicCacheService publicCacheService,
                            com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService ownershipService) {
        this.courseService = courseService;
        this.publicCacheService = publicCacheService;
        this.ownershipService = ownershipService;
    }

    private boolean matchesEtag(String ifNoneMatch, String serverEtag) {
        if (ifNoneMatch == null || serverEtag == null) return false;
        String client = ifNoneMatch.trim();
        String server = serverEtag.trim();
        if (client.equals("*")) return true;
        if (client.startsWith("W/")) client = client.substring(2);
        if (server.startsWith("W/")) server = server.substring(2);
        return client.equals(server);
    }

    // --- Public course browsing ---

    @GetMapping("/courses")
    public ResponseEntity<List<CourseResponseDto>> getCourses(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String search,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        if (teacherId != null || (status != null && !status.equalsIgnoreCase("LIVE"))) {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore().cachePrivate())
                    .body(courseService.getCourses(status, department, teacherId, search));
        }

        PublicCacheService.CacheResult<List<CourseResponseDto>> cached = publicCacheService.getCoursesCatalogue(
                status, department, search,
                () -> courseService.getCourses(status, department, null, search)
        );

        if (matchesEtag(ifNoneMatch, cached.etag())) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(cached.etag())
                    .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic().staleWhileRevalidate(30, TimeUnit.SECONDS))
                    .build();
        }

        return ResponseEntity.ok()
                .eTag(cached.etag())
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic().staleWhileRevalidate(30, TimeUnit.SECONDS))
                .body(cached.data());
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<CourseResponseDto> getCourse(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        Course entity = courseService.getCourseEntity(id);
        if (entity.getStatus() != Course.Status.LIVE) {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore().cachePrivate())
                    .body(courseService.getCourseById(id));
        }

        PublicCacheService.CacheResult<CourseResponseDto> cached = publicCacheService.getCourseDetail(
                id,
                () -> courseService.getCourseById(id)
        );

        if (matchesEtag(ifNoneMatch, cached.etag())) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(cached.etag())
                    .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic().staleWhileRevalidate(30, TimeUnit.SECONDS))
                    .build();
        }

        return ResponseEntity.ok()
                .eTag(cached.etag())
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic().staleWhileRevalidate(30, TimeUnit.SECONDS))
                .body(cached.data());
    }

    @GetMapping("/courses/{id}/lectures")
    public ResponseEntity<List<LectureResponseDto>> getLectures(@PathVariable Long id,
                                                                @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireEnrolled(id, principal);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(courseService.getLectures(id));
    }

    // --- Teacher-only course management ---

    @GetMapping("/teacher/courses")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<CourseResponseDto>> getMyCourses(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(courseService.getCourses(null, null, principal.getUser().getUserId()));
    }

    @PostMapping({"/courses", "/teacher/courses"})
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseResponseDto> createCourse(@RequestBody Course course,
                                                            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(courseService.createCourse(course, principal));
    }

    @PutMapping({"/courses/{id}", "/teacher/courses/{id}"})
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseResponseDto> updateCourse(@PathVariable Long id,
                                                            @RequestBody Course course,
                                                            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(courseService.updateCourse(id, course, principal));
    }

    @DeleteMapping({"/courses/{id}", "/teacher/courses/{id}"})
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id,
                                              @AuthenticationPrincipal CustomUserDetails principal) {
        courseService.deleteCourse(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/courses/{id}/restore", "/teacher/courses/{id}/restore"})
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseResponseDto> restoreCourse(@PathVariable Long id,
                                                           @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(courseService.restoreCourse(id, principal));
    }


    @PostMapping("/teacher/courses/{id}/lectures")
    public ResponseEntity<LectureResponseDto> addLecture(@PathVariable Long id,
                                                           @RequestBody Lecture lecture,
                                                           @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(courseService.addLecture(id, lecture, principal));
    }

    @PostMapping("/teacher/courses/{id}/bulk-lectures")
    public ResponseEntity<List<LectureResponseDto>> addLecturesBulk(@PathVariable Long id,
                                                                      @RequestBody List<Lecture> lectures,
                                                                      @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(courseService.addLecturesBulk(id, lectures, principal));
    }

    @PutMapping("/teacher/lectures/{id}")
    public ResponseEntity<LectureResponseDto> updateLecture(@PathVariable Long id,
                                                              @RequestBody Lecture lecture,
                                                              @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(courseService.updateLecture(id, lecture, principal));
    }

    @DeleteMapping("/teacher/lectures/{id}")
    public ResponseEntity<Void> deleteLecture(@PathVariable Long id,
                                               @AuthenticationPrincipal CustomUserDetails principal) {
        courseService.deleteLecture(id, principal);
        return ResponseEntity.noContent().build();
    }
}
