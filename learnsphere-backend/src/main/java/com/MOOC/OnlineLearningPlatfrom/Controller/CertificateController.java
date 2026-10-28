package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.CertificateResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Certificate;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.CertificateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping
    public ResponseEntity<List<CertificateResponseDto>> getMyCertificates(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null || principal.getUser().getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(certificateService.getCertificatesForUser(principal.getUser().getUserId()));
    }

    @PostMapping("/issue")
    public ResponseEntity<CertificateResponseDto> issue(@RequestBody Map<String, Object> payload,
                                                          @AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null || principal.getUser().getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Object courseIdRaw = payload.get("courseId");
        if (courseIdRaw == null) {
            throw new BadRequestException("courseId is required");
        }
        Long courseId = Long.valueOf(courseIdRaw.toString());
        String typeRaw = payload.getOrDefault("type", "STANDARD").toString();
        Certificate.Type type;
        try {
            type = Certificate.Type.valueOf(typeRaw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid certificate type: " + typeRaw);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(certificateService.issueCertificate(principal.getUser(), courseId, type));
    }

    @GetMapping("/verify/{code}")
    public ResponseEntity<com.MOOC.OnlineLearningPlatfrom.Dto.CertificateVerificationDto> verifyCertificate(@PathVariable String code) {
        var verification = certificateService.verifyCertificate(code);
        var response = ResponseEntity.ok();
        if (verification.isValid()) {
            response.header(HttpHeaders.CACHE_CONTROL, "public, max-age=60");
        } else {
            response.header(HttpHeaders.CACHE_CONTROL, "no-store");
        }
        return response.body(verification);
    }
}
