package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.BookmarkRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.BookmarkResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;

import java.util.List;

public interface BookmarkService {
    List<BookmarkResponseDto> getBookmarks(CustomUserDetails principal);
    List<BookmarkResponseDto> getBookmarksByLecture(Long lectureId, CustomUserDetails principal);
    BookmarkResponseDto createBookmark(BookmarkRequestDto request, CustomUserDetails principal);
    void deleteBookmark(Long id, CustomUserDetails principal);
    void deleteBookmarkByLecture(Long lectureId, CustomUserDetails principal);
}
