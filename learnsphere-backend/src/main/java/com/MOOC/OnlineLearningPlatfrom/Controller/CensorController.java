package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Service.ProfanityFilterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/censor")
public class CensorController {

    public record CensorRequest(String text) {}

    private final ProfanityFilterService profanityFilterService;

    public CensorController(ProfanityFilterService profanityFilterService) {
        this.profanityFilterService = profanityFilterService;
    }

    @PostMapping
    public ResponseEntity<ProfanityFilterService.CensorResult> censorText(@RequestBody CensorRequest request) {
        ProfanityFilterService.CensorResult result = profanityFilterService.censor(request.text());
        return ResponseEntity.ok(result);
    }
}
