package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.BookmarkRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.BookmarkResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Bookmark;
import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.BookmarkRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.LectureRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.BookmarkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final LectureRepository lectureRepository;
    private final CourseContentRepository courseContentRepository;

    public BookmarkServiceImpl(BookmarkRepository bookmarkRepository,
                               LectureRepository lectureRepository,
                               CourseContentRepository courseContentRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.lectureRepository = lectureRepository;
        this.courseContentRepository = courseContentRepository;
    }

    @Override
    public List<BookmarkResponseDto> getBookmarks(CustomUserDetails principal) {
        return bookmarkRepository.findByUser_UserId(principal.getUser().getUserId()).stream()
                .map(BookmarkResponseDto::from)
                .toList();
    }

    @Override
    public List<BookmarkResponseDto> getBookmarksByLecture(Long lectureId, CustomUserDetails principal) {
        return bookmarkRepository.findByUser_UserIdAndLecture_Id(principal.getUser().getUserId(), lectureId).stream()
                .map(BookmarkResponseDto::from)
                .toList();
    }

    @Override
    public List<BookmarkResponseDto> getBookmarksByContent(Long contentId, CustomUserDetails principal) {
        return bookmarkRepository.findByUser_UserIdAndContent_ContentId(principal.getUser().getUserId(), contentId).stream()
                .map(BookmarkResponseDto::from)
                .toList();
    }

    @Override
    public BookmarkResponseDto createBookmark(BookmarkRequestDto request, CustomUserDetails principal) {
        if (request.getContentId() == null && request.getLectureId() == null) {
            throw new BadRequestException("contentId or lectureId is required");
        }

        Bookmark bookmark = new Bookmark();
        bookmark.setUser(principal.getUser());

        CourseContent content = null;
        Lecture lecture = null;

        if (request.getContentId() != null) {
            content = courseContentRepository.findById(request.getContentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + request.getContentId()));
            bookmark.setContent(content);
        }

        if (request.getLectureId() != null) {
            lecture = lectureRepository.findById(request.getLectureId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lecture not found with id: " + request.getLectureId()));
            bookmark.setLecture(lecture);
            if (bookmark.getContent() == null) {
                courseContentRepository.findByLegacyLectureId(request.getLectureId())
                        .ifPresent(bookmark::setContent);
            }
        }

        if (request.getTimestampSeconds() != null) {
            if (request.getTimestampSeconds() < 0) {
                throw new BadRequestException("timestampSeconds must be non-negative");
            }
            Integer maxDuration = null;
            if (bookmark.getContent() != null && bookmark.getContent().getDurationSeconds() != null && bookmark.getContent().getDurationSeconds() > 0) {
                maxDuration = bookmark.getContent().getDurationSeconds();
            } else if (bookmark.getLecture() != null && bookmark.getLecture().getDuration() != null && bookmark.getLecture().getDuration() > 0) {
                maxDuration = bookmark.getLecture().getDuration();
            }
            if (maxDuration != null && request.getTimestampSeconds() > maxDuration) {
                throw new BadRequestException("timestampSeconds cannot exceed content duration (" + maxDuration + "s)");
            }
            bookmark.setTimestampSeconds(request.getTimestampSeconds());
        }

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

    @Override
    @Transactional
    public void deleteBookmarkByLecture(Long lectureId, CustomUserDetails principal) {
        bookmarkRepository.deleteByUser_UserIdAndLecture_Id(principal.getUser().getUserId(), lectureId);
    }

    @Override
    @Transactional
    public void deleteBookmarkByContent(Long contentId, CustomUserDetails principal) {
        bookmarkRepository.deleteByUser_UserIdAndContent_ContentId(principal.getUser().getUserId(), contentId);
    }
}
