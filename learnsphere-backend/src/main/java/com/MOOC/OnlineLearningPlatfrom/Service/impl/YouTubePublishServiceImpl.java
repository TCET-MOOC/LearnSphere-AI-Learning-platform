package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.MediaPublishJob;
import com.MOOC.OnlineLearningPlatfrom.Entity.YouTubeChannelConnection;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ConflictException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.MediaPublishJobRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.YouTubeChannelConnectionRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import com.MOOC.OnlineLearningPlatfrom.Service.YouTubePublishService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class YouTubePublishServiceImpl implements YouTubePublishService {

    private final YouTubeChannelConnectionRepository connectionRepository;
    private final MediaPublishJobRepository jobRepository;
    private final CourseContentRepository courseContentRepository;
    private final OwnershipService ownershipService;
    private final String jwtSecret;

    private final Set<String> usedNonces = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public YouTubePublishServiceImpl(
            YouTubeChannelConnectionRepository connectionRepository,
            MediaPublishJobRepository jobRepository,
            CourseContentRepository courseContentRepository,
            OwnershipService ownershipService,
            @Value("${app.jwt.secret:defaultSecretKey12345678901234567890}") String jwtSecret) {
        this.connectionRepository = connectionRepository;
        this.jobRepository = jobRepository;
        this.courseContentRepository = courseContentRepository;
        this.ownershipService = ownershipService;
        this.jwtSecret = jwtSecret;
    }

    @Override
    public ConnectResponseDto getConnectUrl(CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        long exp = Instant.now().plusSeconds(600).getEpochSecond(); // 10 min
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String payload = userId + ":" + exp + ":" + nonce;
        String sig = computeHmac(payload);
        String state = payload + ":" + sig;

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?"
                + "client_id=learnsphere-youtube-client.apps.googleusercontent.com"
                + "&redirect_uri=http://localhost:8080/api/teacher/media/youtube/callback"
                + "&response_type=code"
                + "&scope=https://www.googleapis.com/auth/youtube.upload%20https://www.googleapis.com/auth/youtube.readonly"
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + state;

        return new ConnectResponseDto(authUrl, state);
    }

    @Override
    public ConnectionDto handleCallback(String code, String state, CustomUserDetails principal) {
        validateState(state);

        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;

        // Mock/simulated Google token exchange for code
        String rawRefreshToken = "rt_" + UUID.randomUUID();
        String encryptedRefreshToken = encryptToken(rawRefreshToken);
        String accessToken = "ya29." + UUID.randomUUID();

        YouTubeChannelConnection connection = connectionRepository.findByUserId(userId)
                .orElseGet(YouTubeChannelConnection::new);

        connection.setUserId(userId);
        connection.setChannelId("UC" + UUID.randomUUID().toString().substring(0, 16));
        connection.setChannelTitle("Teacher Channel (" + (principal != null ? principal.getUsername() : "User") + ")");
        connection.setEncryptedRefreshToken(encryptedRefreshToken);
        connection.setAccessToken(accessToken);
        connection.setTokenExpiresAt(LocalDateTime.now().plusHours(1));
        connection.setConnectedAt(LocalDateTime.now());

        connection = connectionRepository.save(connection);

        return new ConnectionDto(true, connection.getChannelId(), connection.getChannelTitle(), connection.getConnectedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectionDto getConnectionStatus(CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        return connectionRepository.findByUserId(userId)
                .map(conn -> new ConnectionDto(true, conn.getChannelId(), conn.getChannelTitle(), conn.getConnectedAt()))
                .orElse(new ConnectionDto(false, null, null, null));
    }

    @Override
    public void disconnectChannel(CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        connectionRepository.findByUserId(userId).ifPresent(connectionRepository::delete);
    }

    @Override
    public PublishJobResponseDto startPublish(Long contentId, PublishJobRequestDto request, CustomUserDetails principal) {
        ownershipService.requireContentStaff(contentId, principal);

        CourseContent content = courseContentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found: " + contentId));

        if (!"VIDEO".equalsIgnoreCase(content.getContentType()) || !"UPLOAD".equalsIgnoreCase(content.getSourceType())
                || content.getStorageKey() == null || content.getStorageKey().isBlank()) {
            throw new BadRequestException("Only local UPLOAD video content can be published to YouTube");
        }

        Long teacherId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        connectionRepository.findByUserId(teacherId)
                .orElseThrow(() -> new BadRequestException("No connected YouTube channel found for teacher"));

        List<MediaPublishJob> activeJobs = jobRepository.findByContentIdAndStatusIn(contentId, List.of("PENDING", "UPLOADING"));
        if (!activeJobs.isEmpty()) {
            throw new ConflictException("An active publish job is already in progress for this content");
        }

        if (request != null && request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<MediaPublishJob> existing = jobRepository.findByTeacherIdAndContentIdAndIdempotencyKey(
                    teacherId, contentId, request.idempotencyKey());
            if (existing.isPresent()) {
                return toDto(existing.get());
            }
        }

        MediaPublishJob job = new MediaPublishJob();
        job.setContentId(contentId);
        job.setTeacherId(teacherId);
        job.setStatus("PENDING");
        job.setPrivacyStatus(request != null && request.privacyStatus() != null && !request.privacyStatus().isBlank()
                ? request.privacyStatus() : "unlisted");
        job.setIdempotencyKey(request != null ? request.idempotencyKey() : null);

        job = jobRepository.save(job);
        return toDto(job);
    }

    @Override
    @Transactional(readOnly = true)
    public PublishJobResponseDto getJob(Long jobId, CustomUserDetails principal) {
        MediaPublishJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Publish job not found: " + jobId));

        Long currentUserId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        if (!ownershipService.isAdmin(principal) && !job.getTeacherId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Publish job not found: " + jobId);
        }

        return toDto(job);
    }

    @Override
    public PublishJobResponseDto simulateComplete(Long jobId, String youtubeVideoId, CustomUserDetails principal) {
        MediaPublishJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Publish job not found: " + jobId));

        Long currentUserId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        if (!ownershipService.isAdmin(principal) && !job.getTeacherId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Publish job not found: " + jobId);
        }

        job.setStatus("COMPLETED");
        job.setYoutubeVideoId(youtubeVideoId);
        job = jobRepository.save(job);

        // Keep local source active as UPLOAD, but record providerVideoId!
        courseContentRepository.findById(job.getContentId()).ifPresent(content -> {
            content.setProviderVideoId(youtubeVideoId);
            courseContentRepository.save(content);
        });

        return toDto(job);
    }

    @Override
    public PublishJobResponseDto simulateError(Long jobId, String errorMessage, CustomUserDetails principal) {
        MediaPublishJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Publish job not found: " + jobId));

        Long currentUserId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        if (!ownershipService.isAdmin(principal) && !job.getTeacherId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Publish job not found: " + jobId);
        }

        job.setStatus("FAILED");
        job.setErrorMessage(errorMessage);
        job = jobRepository.save(job);

        return toDto(job);
    }

    private void validateState(String state) {
        if (state == null || state.isBlank()) {
            throw new AccessDeniedException("Missing OAuth state");
        }
        String[] parts = state.split(":", -1);
        if (parts.length != 4) {
            throw new AccessDeniedException("Invalid OAuth state format");
        }

        long exp;
        try {
            exp = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("Invalid state expiry");
        }

        if (Instant.now().getEpochSecond() > exp) {
            throw new AccessDeniedException("OAuth state token has expired");
        }

        String payload = parts[0] + ":" + parts[1] + ":" + parts[2];
        String expectedSig = computeHmac(payload);
        if (!MessageDigest.isEqual(expectedSig.getBytes(StandardCharsets.UTF_8), parts[3].getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("Invalid OAuth state signature");
        }

        String nonce = parts[2];
        if (!usedNonces.add(nonce)) {
            throw new BadRequestException("OAuth state token has already been used");
        }
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    private String encryptToken(String token) {
        try {
            byte[] keyBytes = Arrays.copyOf(MessageDigest.getInstance("SHA-256")
                    .digest(jwtSecret.getBytes(StandardCharsets.UTF_8)), 16);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Token encryption failed", e);
        }
    }

    private PublishJobResponseDto toDto(MediaPublishJob job) {
        return new PublishJobResponseDto(
                job.getJobId(),
                job.getContentId(),
                job.getStatus(),
                job.getYoutubeVideoId(),
                job.getErrorMessage(),
                job.getCreatedAt()
        );
    }
}
