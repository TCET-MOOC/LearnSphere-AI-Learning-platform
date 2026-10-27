package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.StoredFile;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher/upload")
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class UploadController {

    private final StorageService storageService;

    public UploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/video")
    public ResponseEntity<Map<String, String>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long ownerId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        StoredFile storedFile = storageService.storeVideo(file, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "fileKey", storedFile.getFileKey(),
                "url", "/api/files/" + storedFile.getFileKey()
        ));
    }

    @PostMapping("/resource")
    public ResponseEntity<Map<String, String>> uploadResource(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long ownerId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        StoredFile storedFile = storageService.storeResource(file, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "fileKey", storedFile.getFileKey(),
                "url", "/api/files/" + storedFile.getFileKey()
        ));
    }
}
