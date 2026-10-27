package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import com.MOOC.OnlineLearningPlatfrom.Service.CourseContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contents")
public class ContentController {

    private final CourseContentService courseContentService;
    private final OwnershipService ownershipService;

    public ContentController(CourseContentService courseContentService,
                             OwnershipService ownershipService) {
        this.courseContentService = courseContentService;
        this.ownershipService = ownershipService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseContent> getContentById(@PathVariable Long id) {
        return ResponseEntity.ok(courseContentService.getContentById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseContent> updateContent(@PathVariable Long id,
                                                        @RequestBody CourseContent content,
                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireContentStaff(id, principal);
        return ResponseEntity.ok(courseContentService.updateContent(id, content));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContent(@PathVariable Long id,
                                              @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireContentStaff(id, principal);
        courseContentService.deleteContent(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reorder")
    public ResponseEntity<CourseContent> reorderContent(@PathVariable Long id,
                                                        @RequestBody(required = false) com.MOOC.OnlineLearningPlatfrom.dto.ReorderRequest request,
                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireContentStaff(id, principal);
        return ResponseEntity.ok(courseContentService.reorderContent(id, request));
    }
}
