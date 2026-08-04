package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.BookmarkRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.BookmarkResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Bookmark;
import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.BookmarkRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.LectureRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.BookmarkService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final LectureRepository lectureRepository;

    public BookmarkServiceImpl(BookmarkRepository bookmarkRepository, LectureRepository lectureRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.lectureRepository = lectureRepository;
    }

    @Override
    public List<BookmarkResponseDto> getBookmarks(CustomUserDetails principal) {
        return bookmarkRepository.findByUser_UserId(principal.getUser().getUserId()).stream()
                .map(BookmarkResponseDto::from)
                .toList();
    }

    @Override
    public BookmarkResponseDto createBookmark(BookmarkRequestDto request, CustomUserDetails principal) {
        if (request.getLectureId() == null) {
            throw new BadRequestException("lectureId is required");
        }
        Lecture lecture = lectureRepository.findById(request.getLectureId())
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found with id: " + request.getLectureId()));

        Bookmark bookmark = new Bookmark();
        bookmark.setUser(principal.getUser());
        bookmark.setLecture(lecture);
        bookmark.setTimestampSeconds(request.getTimestampSeconds());
        bookmark.setLabel(request.getLabel());
        return BookmarkResponseDto.from(bookmarkRepository.save(bookmark));
    }

    @Override
    public void deleteBookmark(Long id, CustomUserDetails principal) {
        Bookmark bookmark = bookmarkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found with id: " + id));
        if (bookmark.getUser() == null || !bookmark.getUser().getUserId().equals(principal.getUser().getUserId())) {
            throw new ResourceNotFoundException("Bookmark not found with id: " + id);
        }
        bookmarkRepository.delete(bookmark);
    }
}
