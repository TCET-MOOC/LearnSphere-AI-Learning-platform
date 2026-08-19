package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.RevenueSummaryDto;
import com.MOOC.OnlineLearningPlatfrom.Service.RevenueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Real revenue breakdown computed from SUCCESS Payment rows: total, by course, by month. */
@RestController
@RequestMapping("/api/admin/revenue")
public class RevenueController {

    private final RevenueService revenueService;

    public RevenueController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }

    @GetMapping
    public ResponseEntity<RevenueSummaryDto> getRevenue() {
        return ResponseEntity.ok(revenueService.getRevenueSummary());
    }
}
