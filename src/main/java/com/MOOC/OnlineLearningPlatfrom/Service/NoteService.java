package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.NoteRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.NoteResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;

import java.util.List;

public interface NoteService {
    List<NoteResponseDto> getNotes(CustomUserDetails principal, Long courseId);
    NoteResponseDto createNote(NoteRequestDto request, CustomUserDetails principal);
    NoteResponseDto updateNote(Long id, NoteRequestDto request, CustomUserDetails principal);
    void deleteNote(Long id, CustomUserDetails principal);
}
