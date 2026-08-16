package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.SentimentSummaryDto;
import com.MOOC.OnlineLearningPlatfrom.Service.SentimentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple keyword-based sentiment analysis over discussion posts + messages.
 * No ML/NLP infrastructure exists in this project - this is a deterministic heuristic.
 */
@RestController
@RequestMapping("/api/admin/sentiment")
public class SentimentController {

    private final SentimentService sentimentService;

    public SentimentController(SentimentService sentimentService) {
        this.sentimentService = sentimentService;
    }

    @GetMapping
    public ResponseEntity<SentimentSummaryDto> getSentiment() {
        return ResponseEntity.ok(sentimentService.getSentiment());
    }
}
