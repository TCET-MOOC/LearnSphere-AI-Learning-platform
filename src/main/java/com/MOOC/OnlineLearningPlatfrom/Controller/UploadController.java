package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/upload")
public class UploadController {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static final List<String> ALLOWED_RESOURCE_TYPES = Arrays.asList(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain", "image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp"
    );

    @PostMapping("/video")
    public ResponseEntity<Map<String, String>> uploadVideo(@RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new BadRequestException("File must be a video");
        }
        return ResponseEntity.ok(Map.of("url", store(file)));
    }

    @PostMapping("/resource")
    public ResponseEntity<Map<String, String>> uploadResource(@RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_RESOURCE_TYPES.contains(contentType)) {
            throw new BadRequestException("Unsupported resource file type: " + contentType);
        }
        return ResponseEntity.ok(Map.of("url", store(file)));
    }

    private String store(MultipartFile file) {
        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String filename = UUID.randomUUID() + "_" + safeName;
            Path target = dir.toPath().resolve(filename);
            Files.copy(file.getInputStream(), target);
            return "/api/uploads/" + filename;
        } catch (IOException e) {
            throw new BadRequestException("Failed to store file: " + e.getMessage());
        }
    }
}
