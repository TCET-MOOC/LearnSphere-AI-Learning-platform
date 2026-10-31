package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.DiscussionThread;
import com.MOOC.OnlineLearningPlatfrom.Entity.Post;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import com.MOOC.OnlineLearningPlatfrom.Service.DiscussionThreadService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for canonical discussion threads and posts.
 * Enforces enrollment gating and ownership (W5-01, W5-02, A-13, A-14, [E-F1, E-F3]).
 */
@RestController
@RequestMapping("/api")
public class DiscussionThreadController {

    private final DiscussionThreadService discussionThreadService;
    private final OwnershipService ownershipService;

    public DiscussionThreadController(DiscussionThreadService discussionThreadService,
                                    OwnershipService ownershipService) {
        this.discussionThreadService = discussionThreadService;
        this.ownershipService = ownershipService;
    }

    // --- Threads ---

    @GetMapping("/courses/{courseId}/threads")
    public ResponseEntity<List<DiscussionThread>> getCourseThreads(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireEnrolled(courseId, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(discussionThreadService.getThreadsForCourse(courseId));
    }

    @PostMapping("/courses/{courseId}/threads")
    public ResponseEntity<DiscussionThread> createThread(
            @PathVariable Long courseId,
            @RequestBody DiscussionThread thread,
            @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireEnrolled(courseId, principal);
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 1L;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(discussionThreadService.createThread(courseId, userId, thread));
    }

    @GetMapping("/threads/{id}")
    public ResponseEntity<DiscussionThread> getThreadById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        DiscussionThread thread = discussionThreadService.getThreadById(id);
        ownershipService.requireEnrolled(thread.getCourseId(), principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(thread);
    }

    @PutMapping("/threads/{id}")
    public ResponseEntity<DiscussionThread> updateThread(
            @PathVariable Long id,
            @RequestBody DiscussionThread thread,
            @AuthenticationPrincipal CustomUserDetails principal) {
        DiscussionThread existing = discussionThreadService.getThreadById(id);
        ownershipService.requireEnrolled(existing.getCourseId(), principal);
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(discussionThreadService.updateThread(id, userId, isAdmin, thread));
    }

    @DeleteMapping("/threads/{id}")
    public ResponseEntity<Void> deleteThread(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        DiscussionThread existing = discussionThreadService.getThreadById(id);
        ownershipService.requireEnrolled(existing.getCourseId(), principal);
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        discussionThreadService.deleteThread(id, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    // --- Posts / Replies ---

    @GetMapping("/threads/{id}/posts")
    public ResponseEntity<List<Post>> getThreadPosts(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        DiscussionThread thread = discussionThreadService.getThreadById(id);
        ownershipService.requireEnrolled(thread.getCourseId(), principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(discussionThreadService.getPostsForThread(id));
    }

    @PostMapping("/threads/{id}/posts")
    public ResponseEntity<Post> createPost(
            @PathVariable Long id,
            @RequestBody Post post,
            @AuthenticationPrincipal CustomUserDetails principal) {
        DiscussionThread thread = discussionThreadService.getThreadById(id);
        ownershipService.requireEnrolled(thread.getCourseId(), principal);
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 1L;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(discussionThreadService.createPost(id, userId, post));
    }

    @PutMapping("/posts/{id}")
    public ResponseEntity<Post> updatePost(
            @PathVariable Long id,
            @RequestBody Post post,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(discussionThreadService.updatePost(id, userId, isAdmin, post));
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
