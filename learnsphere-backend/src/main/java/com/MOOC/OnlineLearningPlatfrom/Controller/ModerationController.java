package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.FlaggedContentDto;
import com.MOOC.OnlineLearningPlatfrom.Service.ModerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Flagged-content moderation queue. GET runs a simple keyword-based auto-flagger over
 * existing discussion posts/messages (seeding real signal, since no user-facing "report"
 * button exists yet) before returning the current pending queue.
 */
@RestController
@RequestMapping("/api/admin/flagged")
public class ModerationController {

    private final ModerationService moderationService;

    public ModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @GetMapping
    public ResponseEntity<List<FlaggedContentDto>> getFlagged(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(moderationService.getFlagged(category));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<FlaggedContentDto> resolve(@PathVariable Long id) {
        return ResponseEntity.ok(moderationService.resolve(id));
    }

    @PutMapping("/{id}/dismiss")
    public ResponseEntity<FlaggedContentDto> dismiss(@PathVariable Long id) {
        return ResponseEntity.ok(moderationService.dismiss(id));
    }
}
