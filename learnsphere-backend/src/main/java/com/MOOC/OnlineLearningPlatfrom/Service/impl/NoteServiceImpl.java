package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.NoteRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.NoteResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;
import com.MOOC.OnlineLearningPlatfrom.Entity.Note;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.LectureRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.NoteRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.NoteService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteServiceImpl implements NoteService {

    private final NoteRepository noteRepository;
    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final CourseContentRepository courseContentRepository;

    public NoteServiceImpl(NoteRepository noteRepository,
                           CourseRepository courseRepository,
                           LectureRepository lectureRepository,
                           CourseContentRepository courseContentRepository) {
        this.noteRepository = noteRepository;
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.courseContentRepository = courseContentRepository;
    }

    @Override
    public List<NoteResponseDto> getNotes(CustomUserDetails principal, Long courseId, Long contentId) {
        Long userId = principal.getUser().getUserId();
        List<Note> notes;
        if (contentId != null) {
            notes = noteRepository.findByUser_UserIdAndCourseContent_ContentId(userId, contentId);
        } else if (courseId != null) {
            notes = noteRepository.findByUser_UserIdAndCourse_Id(userId, courseId);
        } else {
            notes = noteRepository.findByUser_UserId(userId);
        }
        return notes.stream().map(NoteResponseDto::from).toList();
    }

    @Override
    public NoteResponseDto createNote(NoteRequestDto request, CustomUserDetails principal) {
        Note note = new Note();
        note.setUser(principal.getUser());
        applyRequest(note, request);
        return NoteResponseDto.from(noteRepository.save(note));
    }

    @Override
    public NoteResponseDto updateNote(Long id, NoteRequestDto request, CustomUserDetails principal) {
        Note note = getOwnedNote(id, principal);
        applyRequest(note, request);
        return NoteResponseDto.from(noteRepository.save(note));
    }

    @Override
    public void deleteNote(Long id, CustomUserDetails principal) {
        Note note = getOwnedNote(id, principal);
        noteRepository.delete(note);
    }

    private void applyRequest(Note note, NoteRequestDto request) {
        if (request.getContentId() != null) {
            CourseContent content = courseContentRepository.findById(request.getContentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + request.getContentId()));
            note.setCourseContent(content);
            if (content.getModule() != null && content.getModule().getCourse() != null) {
                note.setCourse(content.getModule().getCourse());
            }
        }

        if (request.getLectureId() != null) {
            Lecture lecture = lectureRepository.findById(request.getLectureId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lecture not found with id: " + request.getLectureId()));
            note.setLecture(lecture);
            if (note.getCourseContent() == null) {
                courseContentRepository.findByLegacyLectureId(request.getLectureId())
                        .ifPresent(note::setCourseContent);
            }
            if (note.getCourse() == null && lecture.getCourse() != null) {
                note.setCourse(lecture.getCourse());
            }
        }

        if (request.getCourseId() != null && note.getCourse() == null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));
            note.setCourse(course);
        }

        if (request.getTimestampSeconds() != null) {
            if (request.getTimestampSeconds() < 0) {
                throw new BadRequestException("timestampSeconds must be non-negative");
            }
            Integer maxDuration = null;
            if (note.getCourseContent() != null && note.getCourseContent().getDurationSeconds() != null && note.getCourseContent().getDurationSeconds() > 0) {
                maxDuration = note.getCourseContent().getDurationSeconds();
            } else if (note.getLecture() != null && note.getLecture().getDuration() != null && note.getLecture().getDuration() > 0) {
                maxDuration = note.getLecture().getDuration();
            }
            if (maxDuration != null && request.getTimestampSeconds() > maxDuration) {
                throw new BadRequestException("timestampSeconds cannot exceed content duration (" + maxDuration + "s)");
            }
            note.setTimestampSeconds(request.getTimestampSeconds());
        }

        if (request.getTitle() != null) note.setTitle(request.getTitle());
        if (request.getContent() != null) note.setContent(request.getContent());
        if (request.getTags() != null) note.setTags(request.getTags());
    }

    private Note getOwnedNote(Long id, CustomUserDetails principal) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + id));
        if (note.getUser() == null || !note.getUser().getUserId().equals(principal.getUser().getUserId())) {
            throw new ResourceNotFoundException("Note not found with id: " + id);
        }
        return note;
    }
}
