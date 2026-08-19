package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.PayoutResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.TeacherRoyaltiesResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.RoyaltyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Teacher's own view of their royalty earnings and payout history. */
@RestController
@RequestMapping("/api/teacher")
public class RoyaltyController {

    private final RoyaltyService royaltyService;

    public RoyaltyController(RoyaltyService royaltyService) {
        this.royaltyService = royaltyService;
    }

    @GetMapping("/royalties")
    public ResponseEntity<TeacherRoyaltiesResponseDto> getRoyalties(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(royaltyService.getRoyalties(principal));
    }

    @GetMapping("/payouts")
    public ResponseEntity<List<PayoutResponseDto>> getPayouts(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(royaltyService.getPayouts(principal));
    }
}
