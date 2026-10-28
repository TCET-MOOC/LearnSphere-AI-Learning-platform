package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.StoredFileRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import com.MOOC.OnlineLearningPlatfrom.Service.MediaPlaybackService;
import com.MOOC.OnlineLearningPlatfrom.Service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Service
public class MediaPlaybackServiceImpl implements MediaPlaybackService {

    private final CourseContentRepository courseContentRepository;
    private final OwnershipService ownershipService;
    private final StorageService storageService;
    private final StoredFileRepository storedFileRepository;
    private final String jwtSecret;

    public MediaPlaybackServiceImpl(
            CourseContentRepository courseContentRepository,
            OwnershipService ownershipService,
            StorageService storageService,
            StoredFileRepository storedFileRepository,
            @Value("${app.jwt.secret:defaultSecretKey12345678901234567890}") String jwtSecret) {
        this.courseContentRepository = courseContentRepository;
        this.ownershipService = ownershipService;
        this.storageService = storageService;
        this.storedFileRepository = storedFileRepository;
        this.jwtSecret = jwtSecret;
    }

    @Override
    public PlaybackDescriptorDto getPlaybackDescriptor(Long contentId, CustomUserDetails principal) {
        CourseContent content = courseContentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found: " + contentId));

        if (content.getModule() == null || content.getModule().getCourse() == null) {
            throw new ResourceNotFoundException("Content not associated with a course: " + contentId);
        }

        Long courseId = content.getModule().getCourse().getId();
        ownershipService.requireEnrolled(courseId, principal);

        String sourceType = content.getSourceType();
        if (sourceType == null || sourceType.isBlank()) {
            if (content.getProviderVideoId() != null && !content.getProviderVideoId().isBlank()) {
                sourceType = "YOUTUBE";
            } else if (content.getStorageKey() != null && !content.getStorageKey().isBlank()) {
                sourceType = "UPLOAD";
            } else if (content.getExternalUrl() != null && !content.getExternalUrl().isBlank()) {
                sourceType = "EXTERNAL";
            } else {
                sourceType = "YOUTUBE";
            }
        }

        if ("YOUTUBE".equalsIgnoreCase(sourceType)) {
            String videoId = content.getProviderVideoId();
            if (videoId == null || videoId.isBlank()) {
                videoId = extractYoutubeId(content.getFilePath());
            }
            String embedUrl = "https://www.youtube-nocookie.com/embed/" + (videoId != null ? videoId : "");
            return new PlaybackDescriptorDto(
                    content.getContentId(),
                    content.getTitle(),
                    content.getContentType(),
                    "YOUTUBE",
                    content.getDurationSeconds(),
                    null,
                    videoId,
                    embedUrl,
                    null,
                    null
            );
        } else if ("UPLOAD".equalsIgnoreCase(sourceType)) {
            long exp = Instant.now().plusSeconds(300).getEpochSecond();
            Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
            String sig = computeHmacHex(content.getContentId(), userId, exp);
            String streamUrl = "/api/contents/" + content.getContentId() + "/stream?exp=" + exp + "&u=" + userId + "&sig=" + sig;
            Instant expiresAt = Instant.ofEpochSecond(exp);
            String mimeType = content.getMimeType() != null && !content.getMimeType().isBlank()
                    ? content.getMimeType() : "video/mp4";

            return new PlaybackDescriptorDto(
                    content.getContentId(),
                    content.getTitle(),
                    content.getContentType(),
                    "UPLOAD",
                    content.getDurationSeconds(),
                    mimeType,
                    null,
                    null,
                    streamUrl,
                    expiresAt
            );
        } else if ("EXTERNAL".equalsIgnoreCase(sourceType)) {
            String streamUrl = content.getExternalUrl();
            return new PlaybackDescriptorDto(
                    content.getContentId(),
                    content.getTitle(),
                    content.getContentType(),
                    "EXTERNAL",
                    content.getDurationSeconds(),
                    content.getMimeType(),
                    null,
                    null,
                    streamUrl,
                    null
            );
        }

        throw new BadRequestException("Unsupported source type: " + sourceType);
    }

    @Override
    public void validateStreamSignature(Long contentId, Long userId, long exp, String sig) {
        long now = Instant.now().getEpochSecond();
        if (now > exp) {
            throw new AccessDeniedException("Stream signature has expired");
        }
        if (sig == null || sig.isBlank()) {
            throw new AccessDeniedException("Missing stream signature");
        }
        String expected = computeHmacHex(contentId, userId, exp);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("Invalid stream signature");
        }
    }

    @Override
    public ResponseEntity<?> streamVideo(Long contentId, Long userId, long exp, String sig, String rangeHeader) {
        validateStreamSignature(contentId, userId, exp, sig);

        CourseContent content = courseContentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found: " + contentId));

        if (!"UPLOAD".equalsIgnoreCase(content.getSourceType()) || content.getStorageKey() == null) {
            throw new ResourceNotFoundException("No local media stream found for content: " + contentId);
        }

        Resource resource = storageService.loadAsResource(content.getStorageKey());
        long fileLength;
        try {
            fileLength = resource.contentLength();
        } catch (Exception e) {
            throw new ResourceNotFoundException("Could not determine file size: " + e.getMessage());
        }

        String mimeType = content.getMimeType() != null && !content.getMimeType().isBlank()
                ? content.getMimeType() : "video/mp4";

        if (rangeHeader == null || rangeHeader.isBlank()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileLength))
                    .header(HttpHeaders.CONTENT_TYPE, mimeType)
                    .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                    .body(resource);
        }

        String range = rangeHeader.trim();
        if (range.startsWith("bytes=")) {
            range = range.substring("bytes=".length()).trim();
        }

        long start = 0;
        long end = fileLength - 1;
        String[] parts = range.split("-", -1);
        try {
            if (!parts[0].isEmpty()) {
                start = Long.parseLong(parts[0].trim());
            }
            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = Long.parseLong(parts[1].trim());
            } else if (parts[0].isEmpty() && parts.length > 1) {
                long suffix = Long.parseLong(parts[1].trim());
                start = Math.max(0, fileLength - suffix);
                end = fileLength - 1;
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength)
                    .build();
        }

        if (start > end || start >= fileLength) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength)
                    .build();
        }
        end = Math.min(end, fileLength - 1);
        long contentLength = end - start + 1;

        byte[] data = new byte[(int) contentLength];
        try (InputStream is = resource.getInputStream()) {
            long toSkip = start;
            while (toSkip > 0) {
                long skipped = is.skip(toSkip);
                if (skipped <= 0) {
                    if (is.read() == -1) break;
                    skipped = 1;
                }
                toSkip -= skipped;
            }
            int totalRead = 0;
            while (totalRead < contentLength) {
                int r = is.read(data, totalRead, (int) (contentLength - totalRead));
                if (r == -1) break;
                totalRead += r;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read video range", e);
        }

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(new ByteArrayResource(data));
    }

    private String computeHmacHex(Long contentId, Long userId, long exp) {
        try {
            String payload = contentId + ":" + userId + ":" + exp;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC signature", e);
        }
    }

    private String extractYoutubeId(String url) {
        if (url == null || url.isBlank()) return "";
        if (url.contains("v=")) {
            int start = url.indexOf("v=") + 2;
            int end = url.indexOf('&', start);
            return end == -1 ? url.substring(start) : url.substring(start, end);
        }
        if (url.contains("youtu.be/")) {
            int start = url.indexOf("youtu.be/") + 9;
            int end = url.indexOf('?', start);
            return end == -1 ? url.substring(start) : url.substring(start, end);
        }
        return url;
    }
}
