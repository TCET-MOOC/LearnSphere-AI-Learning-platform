package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.CourseResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;
import com.MOOC.OnlineLearningPlatfrom.Service.PublicCacheService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * W2-11: In-process Caffeine cache implementation for public read models.
 * Rules: NFR-02, NFR-03, A-04, W2-02, W2-03.
 *
 * Implements:
 * 1. Caffeine in-process caching for published catalogue, course detail, departments, and department courses.
 * 2. TTL <= 60 s.
 * 3. Deterministic ETags derived from per-key cache generation.
 * 4. Hit/miss metrics recording.
 * 5. Comprehensive cache invalidation on curriculum and reference mutations.
 */
@Service
public class PublicCacheServiceImpl implements PublicCacheService {

    private final Cache<String, List<CourseResponseDto>> catalogueCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(1000)
            .recordStats()
            .build();

    private final Cache<Long, CourseResponseDto> courseDetailCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(1000)
            .recordStats()
            .build();

    private final Cache<String, List<Department>> departmentsCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(100)
            .recordStats()
            .build();

    private final Cache<Long, Department> departmentDetailCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(100)
            .recordStats()
            .build();

    private final Cache<Long, List<CourseResponseDto>> departmentCoursesCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(500)
            .recordStats()
            .build();

    private final ConcurrentHashMap<String, AtomicLong> generations = new ConcurrentHashMap<>();

    private long getGeneration(String key) {
        return generations.computeIfAbsent(key, k -> new AtomicLong(1)).get();
    }

    private void bumpGeneration(String key) {
        generations.computeIfAbsent(key, k -> new AtomicLong(1)).incrementAndGet();
    }

    @Override
    public CacheResult<List<CourseResponseDto>> getCoursesCatalogue(
            String status, String department, String search,
            Supplier<List<CourseResponseDto>> loader) {
        String cacheKey = (status != null ? status : "LIVE") + "|"
                + (department != null ? department : "ALL") + "|"
                + (search != null ? search : "");

        List<CourseResponseDto> data = catalogueCache.get(cacheKey, k -> loader.get());
        String etag = "\"courses-cat-g" + getGeneration("catalogue") + "-" + Integer.toHexString(cacheKey.hashCode()) + "\"";
        return new CacheResult<>(data, etag);
    }

    @Override
    public CacheResult<CourseResponseDto> getCourseDetail(
            Long courseId,
            Supplier<CourseResponseDto> loader) {
        CourseResponseDto data = courseDetailCache.get(courseId, k -> loader.get());
        String etag = "\"course-" + courseId + "-g" + getGeneration("course:" + courseId) + "\"";
        return new CacheResult<>(data, etag);
    }

    @Override
    public CacheResult<List<Department>> getDepartments(
            Supplier<List<Department>> loader) {
        List<Department> data = departmentsCache.get("all", k -> loader.get());
        String etag = "\"departments-g" + getGeneration("departments") + "\"";
        return new CacheResult<>(data, etag);
    }

    @Override
    public CacheResult<Department> getDepartmentDetail(
            Long departmentId,
            Supplier<Department> loader) {
        Department data = departmentDetailCache.get(departmentId, k -> loader.get());
        String etag = "\"department-" + departmentId + "-g" + getGeneration("department:" + departmentId) + "\"";
        return new CacheResult<>(data, etag);
    }

    @Override
    public CacheResult<List<CourseResponseDto>> getDepartmentCourses(
            Long departmentId,
            Supplier<List<CourseResponseDto>> loader) {
        List<CourseResponseDto> data = departmentCoursesCache.get(departmentId, k -> loader.get());
        String etag = "\"dept-courses-" + departmentId + "-g" + getGeneration("department_courses:" + departmentId) + "\"";
        return new CacheResult<>(data, etag);
    }

    @Override
    public void invalidateCourse(Long courseId) {
        if (courseId != null) {
            courseDetailCache.invalidate(courseId);
            bumpGeneration("course:" + courseId);
        }
        catalogueCache.invalidateAll();
        bumpGeneration("catalogue");
        departmentCoursesCache.invalidateAll();
        bumpGeneration("department_courses");
    }

    @Override
    public void invalidateAllCourses() {
        courseDetailCache.invalidateAll();
        catalogueCache.invalidateAll();
        departmentCoursesCache.invalidateAll();
        bumpGeneration("catalogue");
        bumpGeneration("courses_all");
    }

    @Override
    public void invalidateDepartments() {
        departmentsCache.invalidateAll();
        departmentDetailCache.invalidateAll();
        departmentCoursesCache.invalidateAll();
        bumpGeneration("departments");
    }

    @Override
    public void invalidateDepartment(Long departmentId) {
        departmentsCache.invalidateAll();
        if (departmentId != null) {
            departmentDetailCache.invalidate(departmentId);
            departmentCoursesCache.invalidate(departmentId);
            bumpGeneration("department:" + departmentId);
            bumpGeneration("department_courses:" + departmentId);
        }
        bumpGeneration("departments");
    }

    @Override
    public long getHitCount() {
        return catalogueCache.stats().hitCount()
                + courseDetailCache.stats().hitCount()
                + departmentsCache.stats().hitCount()
                + departmentDetailCache.stats().hitCount()
                + departmentCoursesCache.stats().hitCount();
    }

    @Override
    public long getMissCount() {
        return catalogueCache.stats().missCount()
                + courseDetailCache.stats().missCount()
                + departmentsCache.stats().missCount()
                + departmentDetailCache.stats().missCount()
                + departmentCoursesCache.stats().missCount();
    }
}
