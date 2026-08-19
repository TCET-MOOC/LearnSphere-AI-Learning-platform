package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.ReportsSummaryDto;
import com.MOOC.OnlineLearningPlatfrom.Service.ReportsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Real aggregate platform snapshot backing the admin "Reports" screen and CSV export. */
@RestController
@RequestMapping("/api/admin/reports")
public class ReportsController {

    private final ReportsService reportsService;

    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ReportsSummaryDto> getSummary() {
        return ResponseEntity.ok(reportsService.getSummary());
    }
}
