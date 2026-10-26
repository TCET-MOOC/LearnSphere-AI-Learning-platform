package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Service.CourseContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contents")
public class ContentController {

    private final CourseContentService courseContentService;

    public ContentController(CourseContentService courseContentService) {
        this.courseContentService = courseContentService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseContent> getContentById(@PathVariable Long id) {
        return ResponseEntity.ok(courseContentService.getContentById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseContent> updateContent(@PathVariable Long id, @RequestBody CourseContent content) {
        return ResponseEntity.ok(courseContentService.updateContent(id, content));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContent(@PathVariable Long id) {
        courseContentService.deleteContent(id);
        return ResponseEntity.noContent().build();
    }
}
