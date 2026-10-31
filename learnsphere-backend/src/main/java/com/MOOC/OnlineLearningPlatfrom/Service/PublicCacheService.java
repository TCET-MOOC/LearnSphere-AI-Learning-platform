package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.CourseResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * W2-11 / W5-09: Public read cache service managing Caffeine DTO caches and per-key generation tracking for ETags.
 * Rules: NFR-02, NFR-03, A-04, W2-02, W2-03, W5-09.
 */
public interface PublicCacheService {

    record CacheResult<T>(T data, String etag) {}

    CacheResult<List<CourseResponseDto>> getCoursesCatalogue(
            String status, String department, String search,
            Supplier<List<CourseResponseDto>> loader);

    CacheResult<CourseResponseDto> getCourseDetail(
            Long courseId,
            Supplier<CourseResponseDto> loader);

    CacheResult<List<Department>> getDepartments(
            Supplier<List<Department>> loader);

    CacheResult<Department> getDepartmentDetail(
            Long departmentId,
            Supplier<Department> loader);

    CacheResult<List<CourseResponseDto>> getDepartmentCourses(
            Long departmentId,
            Supplier<List<CourseResponseDto>> loader);

    CacheResult<Map<String, Object>> getCourseFeedbackAvg(
            Long courseId,
            Supplier<Map<String, Object>> loader);

    void invalidateCourse(Long courseId);

    void invalidateAllCourses();

    void invalidateDepartments();

    void invalidateDepartment(Long departmentId);

    void invalidateCourseFeedbackAvg(Long courseId);

    long getHitCount();

    long getMissCount();
}
