package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.Assignment;
import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import com.MOOC.OnlineLearningPlatfrom.Service.AssignmentService;
import com.MOOC.OnlineLearningPlatfrom.Service.CourseContentService;
import com.MOOC.OnlineLearningPlatfrom.Service.ModuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ModuleController {

    private final ModuleService moduleService;
    private final CourseContentService courseContentService;
    private final AssignmentService assignmentService;
    private final OwnershipService ownershipService;

    public ModuleController(ModuleService moduleService,
                            CourseContentService courseContentService,
                            AssignmentService assignmentService,
                            OwnershipService ownershipService) {
        this.moduleService = moduleService;
        this.courseContentService = courseContentService;
        this.assignmentService = assignmentService;
        this.ownershipService = ownershipService;
    }

    // --- Course-scoped Module endpoints ---

    @GetMapping("/courses/{courseId}/modules")
    public ResponseEntity<List<Module>> getModulesByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(moduleService.getModulesByCourseId(courseId));
    }

    @PostMapping("/courses/{courseId}/modules")
    public ResponseEntity<Module> createModule(@PathVariable Long courseId,
                                                @RequestBody Module module,
                                                @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireCourseStaff(courseId, principal);
        return ResponseEntity.ok(moduleService.createModule(courseId, module));
    }

    // --- Module-scoped endpoints ---

    @GetMapping("/modules/{id}")
    public ResponseEntity<Module> getModuleById(@PathVariable Long id) {
        return ResponseEntity.ok(moduleService.getModuleById(id));
    }

    @PutMapping("/modules/{id}")
    public ResponseEntity<Module> updateModule(@PathVariable Long id,
                                                @RequestBody Module module,
                                                @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireModuleStaff(id, principal);
        return ResponseEntity.ok(moduleService.updateModule(id, module));
    }

    @DeleteMapping("/modules/{id}")
    public ResponseEntity<Void> deleteModule(@PathVariable Long id,
                                             @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireModuleStaff(id, principal);
        moduleService.deleteModule(id);
        return ResponseEntity.noContent().build();
    }

    // --- Module contents and assignments ---

    @GetMapping("/modules/{id}/contents")
    public ResponseEntity<List<CourseContent>> getModuleContents(@PathVariable Long id) {
        return ResponseEntity.ok(moduleService.getContentsByModuleId(id));
    }

    @PostMapping("/modules/{id}/contents")
    public ResponseEntity<CourseContent> addContentToModule(@PathVariable Long id,
                                                             @RequestBody CourseContent content,
                                                             @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireModuleStaff(id, principal);
        return ResponseEntity.ok(courseContentService.addContentToModule(id, content));
    }

    @GetMapping("/modules/{id}/assignments")
    public ResponseEntity<List<Assignment>> getAssignmentsByModule(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByModuleId(id));
    }

    @PostMapping("/modules/{id}/assignments")
    public ResponseEntity<Assignment> createAssignmentUnderModule(@PathVariable Long id,
                                                                   @RequestBody Assignment assignment,
                                                                   @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireModuleStaff(id, principal);
        return ResponseEntity.ok(assignmentService.createAssignment(id, assignment));
    }
}
