package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.DiscussionThread;
import com.MOOC.OnlineLearningPlatfrom.Entity.Post;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.DiscussionThreadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DiscussionThreadController {

    private final DiscussionThreadService discussionThreadService;

    public DiscussionThreadController(DiscussionThreadService discussionThreadService) {
        this.discussionThreadService = discussionThreadService;
    }

    // --- Threads ---

    @GetMapping("/courses/{courseId}/threads")
    public ResponseEntity<List<DiscussionThread>> getCourseThreads(@PathVariable Long courseId) {
        return ResponseEntity.ok(discussionThreadService.getThreadsForCourse(courseId));
    }

    @PostMapping("/courses/{courseId}/threads")
    public ResponseEntity<DiscussionThread> createThread(
            @PathVariable Long courseId,
            @RequestBody DiscussionThread thread,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 1L;
        return ResponseEntity.ok(discussionThreadService.createThread(courseId, userId, thread));
    }

    @GetMapping("/threads/{id}")
    public ResponseEntity<DiscussionThread> getThreadById(@PathVariable Long id) {
        return ResponseEntity.ok(discussionThreadService.getThreadById(id));
    }

    @PutMapping("/threads/{id}")
    public ResponseEntity<DiscussionThread> updateThread(
            @PathVariable Long id,
            @RequestBody DiscussionThread thread,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(discussionThreadService.updateThread(id, userId, isAdmin, thread));
    }

    @DeleteMapping("/threads/{id}")
    public ResponseEntity<Void> deleteThread(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        discussionThreadService.deleteThread(id, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    // --- Posts / Replies ---

    @GetMapping("/threads/{id}/posts")
    public ResponseEntity<List<Post>> getThreadPosts(@PathVariable Long id) {
        return ResponseEntity.ok(discussionThreadService.getPostsForThread(id));
    }

    @PostMapping("/threads/{id}/posts")
    public ResponseEntity<Post> createPost(
            @PathVariable Long id,
            @RequestBody Post post,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 1L;
        return ResponseEntity.ok(discussionThreadService.createPost(id, userId, post));
    }

    @PutMapping("/posts/{id}")
    public ResponseEntity<Post> updatePost(
            @PathVariable Long id,
            @RequestBody Post post,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(discussionThreadService.updatePost(id, userId, isAdmin, post));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        discussionThreadService.deletePost(id, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }
}
