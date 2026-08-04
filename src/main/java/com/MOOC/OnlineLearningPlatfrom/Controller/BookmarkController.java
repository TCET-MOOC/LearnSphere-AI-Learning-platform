package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.BookmarkRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.BookmarkResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.BookmarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @GetMapping
    public ResponseEntity<List<BookmarkResponseDto>> getBookmarks(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(bookmarkService.getBookmarks(principal));
    }

    @PostMapping
    public ResponseEntity<BookmarkResponseDto> createBookmark(@RequestBody BookmarkRequestDto request,
                                                                @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(bookmarkService.createBookmark(request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBookmark(@PathVariable Long id,
                                                @AuthenticationPrincipal CustomUserDetails principal) {
        bookmarkService.deleteBookmark(id, principal);
        return ResponseEntity.noContent().build();
    }
}
