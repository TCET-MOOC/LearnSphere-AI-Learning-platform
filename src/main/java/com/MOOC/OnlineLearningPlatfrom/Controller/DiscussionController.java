package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.DiscussionPostDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.DiscussionPostRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.DiscussionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @GetMapping("/api/courses/{courseId}/discussion")
    public ResponseEntity<List<DiscussionPostDto>> getCourseDiscussion(@PathVariable Long courseId) {
        return ResponseEntity.ok(discussionService.getCourseDiscussion(courseId));
    }

    @GetMapping("/api/lectures/{lectureId}/discussion")
    public ResponseEntity<List<DiscussionPostDto>> getLectureDiscussion(@PathVariable Long lectureId) {
        return ResponseEntity.ok(discussionService.getLectureDiscussion(lectureId));
    }

    @PostMapping("/api/discussion")
    public ResponseEntity<DiscussionPostDto> createPost(@RequestBody DiscussionPostRequestDto request,
                                                          @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(discussionService.createPost(request, principal));
    }

    @DeleteMapping("/api/discussion/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id,
                                            @AuthenticationPrincipal CustomUserDetails principal) {
        discussionService.deletePost(id, principal);
        return ResponseEntity.noContent().build();
    }
}
