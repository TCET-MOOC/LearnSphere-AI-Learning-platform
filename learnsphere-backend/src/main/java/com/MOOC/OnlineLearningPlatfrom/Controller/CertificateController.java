package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.CertificateResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Certificate;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.CertificateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
        return ResponseEntity.ok(certificateService.getCertificatesForUser(principal.getUser().getUserId()));
    }

    @PostMapping("/issue")
    public ResponseEntity<CertificateResponseDto> issue(@RequestBody Map<String, Object> payload,
                                                          @AuthenticationPrincipal CustomUserDetails principal) {
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
        return ResponseEntity.ok(certificateService.issueCertificate(principal.getUser(), courseId, type));
    }

    @GetMapping("/verify/{code}")
    public ResponseEntity<com.MOOC.OnlineLearningPlatfrom.Dto.CertificateVerificationDto> verifyCertificate(@PathVariable String code) {
        return ResponseEntity.ok(certificateService.verifyCertificate(code));
    }
}
