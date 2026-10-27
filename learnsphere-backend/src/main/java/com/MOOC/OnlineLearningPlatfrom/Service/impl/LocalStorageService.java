package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.StoredFile;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.StoredFileRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    public static final long MAX_FILE_SIZE = 200 * 1024 * 1024L; // 200 MB cap

    private static final List<String> ALLOWED_RESOURCE_MIME = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/gif",
            "image/webp"
    );

    private static final List<String> ALLOWED_VIDEO_MIME = Arrays.asList(
            "video/mp4",
            "video/webm",
            "video/quicktime",
            "video/x-matroska"
    );

    @Value("${app.upload.dir}")
    private String uploadDir;

    private final StoredFileRepository storedFileRepository;

    public LocalStorageService(StoredFileRepository storedFileRepository) {
        this.storedFileRepository = storedFileRepository;
    }

    @Override
    public StoredFile storeResource(MultipartFile file, Long ownerId) {
        validateFile(file, ALLOWED_RESOURCE_MIME, false);
        return doStore(file, ownerId);
    }

    @Override
    public StoredFile storeVideo(MultipartFile file, Long ownerId) {
        validateFile(file, ALLOWED_VIDEO_MIME, true);
        return doStore(file, ownerId);
    }

    @Override
    public StoredFile getMetadata(String fileKey) {
        if (fileKey == null || fileKey.isBlank() || fileKey.contains("..") || fileKey.contains("/") || fileKey.contains("\\")) {
            throw new ResourceNotFoundException("File not found with key: " + fileKey);
        }
        return storedFileRepository.findByFileKey(fileKey)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with key: " + fileKey));
    }

    @Override
    public Resource loadAsResource(String fileKey) {
        StoredFile metadata = getMetadata(fileKey);
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = root.resolve(metadata.getStoragePath()).normalize();

        // Traversal guard
        if (!target.startsWith(root)) {
            throw new BadRequestException("Path traversal attempt detected");
        }

        File file = target.toFile();
        if (!file.exists() || !file.canRead()) {
            throw new ResourceNotFoundException("File not found on storage: " + fileKey);
        }

        return new FileSystemResource(file);
    }

    @Override
    public void delete(String fileKey) {
        StoredFile metadata = getMetadata(fileKey);
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = root.resolve(metadata.getStoragePath()).normalize();

        if (target.startsWith(root)) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
            }
        }
        storedFileRepository.delete(metadata);
    }

    private void validateFile(MultipartFile file, List<String> allowedMimes, boolean isVideo) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File cannot be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds 200MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new BadRequestException("Missing content type");
        }
        contentType = contentType.toLowerCase(Locale.ROOT).trim();
        // Extract base mime type before any parameters (e.g. text/plain; charset=UTF-8)
        if (contentType.contains(";")) {
            contentType = contentType.split(";")[0].trim();
        }

        if (!allowedMimes.contains(contentType)) {
            throw new BadRequestException("Unsupported file type: " + contentType);
        }

        byte[] header = new byte[512];
        int read;
        try (InputStream is = file.getInputStream()) {
            read = is.read(header);
        } catch (IOException e) {
            throw new BadRequestException("Cannot read file header: " + e.getMessage());
        }

        if (read < 4) {
            throw new BadRequestException("File header is too small to verify");
        }

        // Check for executable signatures (always rejected)
        if (isExecutable(header, read)) {
            throw new BadRequestException("Executable files and scripts are forbidden");
        }

        if (isVideo) {
            if (!isValidVideoMagicBytes(header, read, contentType)) {
                throw new BadRequestException("File content does not match video format: " + contentType);
            }
        } else {
            if (!isValidResourceMagicBytes(header, read, contentType)) {
                throw new BadRequestException("File content does not match resource format: " + contentType);
            }
        }
    }

    private boolean isExecutable(byte[] header, int read) {
        // Windows PE executable MZ
        if (read >= 2 && header[0] == 0x4D && header[1] == 0x5A) {
            return true;
        }
        // Linux ELF
        if (read >= 4 && header[0] == 0x7F && header[1] == 'E' && header[2] == 'L' && header[3] == 'F') {
            return true;
        }
        // Shell script #!
        if (read >= 2 && header[0] == '#' && header[1] == '!') {
            return true;
        }
        return false;
    }

    private boolean isValidVideoMagicBytes(byte[] header, int read, String contentType) {
        // MP4 / MOV: bytes 4..7 'ftyp', 'moov', 'mdat'
        if (contentType.equals("video/mp4") || contentType.equals("video/quicktime")) {
            if (read >= 8) {
                String tag = new String(header, 4, 4, StandardCharsets.ISO_8859_1);
                if ("ftyp".equalsIgnoreCase(tag) || "moov".equalsIgnoreCase(tag) || "mdat".equalsIgnoreCase(tag)) {
                    return true;
                }
            }
        }
        // WebM / Matroska: 0x1A 0x45 0xDF 0xA3
        if (contentType.equals("video/webm") || contentType.equals("video/x-matroska")) {
            if (read >= 4 &&
                    (header[0] & 0xFF) == 0x1A &&
                    (header[1] & 0xFF) == 0x45 &&
                    (header[2] & 0xFF) == 0xDF &&
                    (header[3] & 0xFF) == 0xA3) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidResourceMagicBytes(byte[] header, int read, String contentType) {
        switch (contentType) {
            case "application/pdf":
                // %PDF-
                return read >= 5 && header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F' && header[4] == '-';
            case "image/png":
                // 89 50 4E 47 0D 0A 1A 0A
                return read >= 8 &&
                        (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G' &&
                        header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A;
            case "image/jpeg":
            case "image/jpg":
                // FF D8 FF
                return read >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            case "image/gif":
                // GIF8
                return read >= 4 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8';
            case "image/webp":
                // RIFF....WEBP
                if (read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F') {
                    return header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
                }
                return false;
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation":
                // PK 03 04
                return read >= 4 && header[0] == 'P' && header[1] == 'K' && header[2] == 0x03 && header[3] == 0x04;
            case "application/msword":
            case "application/vnd.ms-powerpoint":
                // Compound document D0 CF 11 E0 A1 B1 1A E1 or PK if pptx/docx mislabelled
                if (read >= 8 && (header[0] & 0xFF) == 0xD0 && (header[1] & 0xFF) == 0xCF &&
                        (header[2] & 0xFF) == 0x11 && (header[3] & 0xFF) == 0xE0) {
                    return true;
                }
                return read >= 4 && header[0] == 'P' && header[1] == 'K' && header[2] == 0x03 && header[3] == 0x04;
            case "text/plain":
                // Must not contain binary null byte in first bytes
                for (int i = 0; i < read; i++) {
                    if (header[i] == 0x00) {
                        return false;
                    }
                }
                return true;
            default:
                return false;
        }
    }

    private StoredFile doStore(MultipartFile file, Long ownerId) {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        // Sanitize original filename (remove directory components)
        originalFilename = Paths.get(originalFilename).getFileName().toString();

        String ext = "";
        int dot = originalFilename.lastIndexOf('.');
        if (dot >= 0) {
            String rawExt = originalFilename.substring(dot).toLowerCase(Locale.ROOT);
            if (rawExt.matches("\\.[a-z0-9]{1,10}")) {
                ext = rawExt;
            }
        }

        String fileKey = UUID.randomUUID().toString().replace("-", "");
        String diskFileName = UUID.randomUUID() + ext;
        String relativePath = ownerId + File.separator + diskFileName;

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path ownerFolder = root.resolve(String.valueOf(ownerId)).normalize();
        Path targetPath = root.resolve(relativePath).normalize();

        // Traversal guard
        if (!targetPath.startsWith(root)) {
            throw new BadRequestException("Invalid storage path");
        }

        try {
            if (!Files.exists(ownerFolder)) {
                Files.createDirectories(ownerFolder);
            }
            Files.copy(file.getInputStream(), targetPath);
        } catch (IOException e) {
            throw new BadRequestException("Failed to write file to disk: " + e.getMessage());
        }

        StoredFile storedFile = new StoredFile();
        storedFile.setFileKey(fileKey);
        storedFile.setOwnerId(ownerId);
        storedFile.setOriginalFilename(originalFilename);
        storedFile.setContentType(file.getContentType());
        storedFile.setSizeBytes(file.getSize());
        storedFile.setStoragePath(relativePath);

        return storedFileRepository.save(storedFile);
    }
}
