package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.NoteRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.NoteResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;
import com.MOOC.OnlineLearningPlatfrom.Entity.Note;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
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

    public NoteServiceImpl(NoteRepository noteRepository, CourseRepository courseRepository,
                            LectureRepository lectureRepository) {
        this.noteRepository = noteRepository;
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
    }

    @Override
    public List<NoteResponseDto> getNotes(CustomUserDetails principal, Long courseId) {
        Long userId = principal.getUser().getUserId();
        List<Note> notes = courseId != null
                ? noteRepository.findByUser_UserIdAndCourse_Id(userId, courseId)
                : noteRepository.findByUser_UserId(userId);
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
        if (request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));
            note.setCourse(course);
        }
        if (request.getLectureId() != null) {
            Lecture lecture = lectureRepository.findById(request.getLectureId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lecture not found with id: " + request.getLectureId()));
            note.setLecture(lecture);
        }
        if (request.getTimestampSeconds() != null) note.setTimestampSeconds(request.getTimestampSeconds());
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
