package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.AffiliationRequestResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.CollegeResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.College;
import com.MOOC.OnlineLearningPlatfrom.Service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/colleges")
    public ResponseEntity<List<CollegeResponseDto>> getColleges() {
        return ResponseEntity.ok(adminService.getAllColleges());
    }

    @PostMapping("/colleges")
    public ResponseEntity<CollegeResponseDto> createCollege(@RequestBody College college) {
        return ResponseEntity.ok(adminService.createCollege(college));
    }

    @PutMapping("/colleges/{collegeId}/verify")
    public ResponseEntity<Map<String, Boolean>> verifyCollege(@PathVariable Long collegeId) {
        adminService.verifyCollege(collegeId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/colleges/{collegeId}")
    public ResponseEntity<Map<String, Boolean>> rejectCollege(@PathVariable Long collegeId) {
        adminService.rejectCollege(collegeId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/colleges/affiliations")
    public ResponseEntity<List<AffiliationRequestResponseDto>> getAffiliationRequests() {
        return ResponseEntity.ok(adminService.getAffiliationRequests());
    }

    @PutMapping("/colleges/affiliations/{requestId}/approve")
    public ResponseEntity<Map<String, Boolean>> approveAffiliation(@PathVariable Long requestId) {
        adminService.approveAffiliation(requestId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/colleges/affiliations/{requestId}/reject")
    public ResponseEntity<Map<String, Boolean>> rejectAffiliation(@PathVariable Long requestId) {
        adminService.rejectAffiliation(requestId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
