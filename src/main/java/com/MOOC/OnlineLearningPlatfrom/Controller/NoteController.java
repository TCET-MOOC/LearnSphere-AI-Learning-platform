package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.NoteRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.NoteResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.NoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public ResponseEntity<List<NoteResponseDto>> getNotes(@RequestParam(required = false) Long courseId,
                                                            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(noteService.getNotes(principal, courseId));
    }

    @PostMapping
    public ResponseEntity<NoteResponseDto> createNote(@RequestBody NoteRequestDto request,
                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(noteService.createNote(request, principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponseDto> updateNote(@PathVariable Long id,
                                                        @RequestBody NoteRequestDto request,
                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(noteService.updateNote(id, request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id,
                                            @AuthenticationPrincipal CustomUserDetails principal) {
        noteService.deleteNote(id, principal);
        return ResponseEntity.noContent().build();
    }
}
