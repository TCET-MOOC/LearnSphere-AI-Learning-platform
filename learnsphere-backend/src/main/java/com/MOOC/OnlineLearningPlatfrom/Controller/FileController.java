package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.StoredFile;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import com.MOOC.OnlineLearningPlatfrom.Service.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final StorageService storageService;
    private final OwnershipService ownershipService;

    public FileController(StorageService storageService, OwnershipService ownershipService) {
        this.storageService = storageService;
        this.ownershipService = ownershipService;
    }

    @GetMapping("/{key}")
    public ResponseEntity<Resource> getFile(
            @PathVariable String key,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (principal == null || principal.getUser() == null) {
            throw new ResourceNotFoundException("File not found with key: " + key);
        }

        StoredFile metadata = storageService.getMetadata(key);
        Long currentUserId = principal.getUser().getUserId();

        // IDOR protection: only owner or admin (or entitled user) can access
        boolean isOwner = metadata.getOwnerId().equals(currentUserId);
        boolean isAdmin = ownershipService.isAdmin(principal);

        if (!isOwner && !isAdmin) {
            throw new ResourceNotFoundException("File not found with key: " + key);
        }

        Resource resource = storageService.loadAsResource(key);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(metadata.getContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.getOriginalFilename() + "\"")
                .body(resource);
    }
}
