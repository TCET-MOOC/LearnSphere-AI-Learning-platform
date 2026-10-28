package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.CourseResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;
import com.MOOC.OnlineLearningPlatfrom.Service.DepartmentService;
import com.MOOC.OnlineLearningPlatfrom.Service.PublicCacheService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final PublicCacheService publicCacheService;

    public DepartmentController(DepartmentService departmentService, PublicCacheService publicCacheService) {
        this.departmentService = departmentService;
        this.publicCacheService = publicCacheService;
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

    @GetMapping
    public ResponseEntity<List<Department>> getAllDepartments(
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        PublicCacheService.CacheResult<List<Department>> cached = publicCacheService.getDepartments(
                () -> departmentService.getAllDepartments()
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

    @GetMapping("/{id}")
    public ResponseEntity<Department> getDepartmentById(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        PublicCacheService.CacheResult<Department> cached = publicCacheService.getDepartmentDetail(
                id,
                () -> departmentService.getDepartmentById(id)
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

    @PostMapping
    public ResponseEntity<Department> createDepartment(@RequestBody Department department) {
        Department created = departmentService.createDepartment(department);
        publicCacheService.invalidateDepartments();
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Department> updateDepartment(@PathVariable Long id, @RequestBody Department department) {
        Department updated = departmentService.updateDepartment(id, department);
        publicCacheService.invalidateDepartment(id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        publicCacheService.invalidateDepartment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/courses")
    public ResponseEntity<List<CourseResponseDto>> getCoursesByDepartment(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        PublicCacheService.CacheResult<List<CourseResponseDto>> cached = publicCacheService.getDepartmentCourses(
                id,
                () -> departmentService.getCoursesByDepartmentId(id)
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
}


