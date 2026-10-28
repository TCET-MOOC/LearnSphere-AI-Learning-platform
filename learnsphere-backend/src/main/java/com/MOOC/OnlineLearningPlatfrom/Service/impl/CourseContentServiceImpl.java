package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.ModuleRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.CourseContentService;
import com.MOOC.OnlineLearningPlatfrom.dto.ReorderRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourseContentServiceImpl implements CourseContentService {

    private final CourseContentRepository courseContentRepository;
    private final ModuleRepository moduleRepository;
    private final com.MOOC.OnlineLearningPlatfrom.Repository.StoredFileRepository storedFileRepository;
    private final com.MOOC.OnlineLearningPlatfrom.Service.PublicCacheService publicCacheService;

    @org.springframework.beans.factory.annotation.Autowired
    public CourseContentServiceImpl(CourseContentRepository courseContentRepository,
                                    ModuleRepository moduleRepository,
                                    com.MOOC.OnlineLearningPlatfrom.Repository.StoredFileRepository storedFileRepository,
                                    @org.springframework.beans.factory.annotation.Autowired(required = false)
                                    com.MOOC.OnlineLearningPlatfrom.Service.PublicCacheService publicCacheService) {
        this.courseContentRepository = courseContentRepository;
        this.moduleRepository = moduleRepository;
        this.storedFileRepository = storedFileRepository;
        this.publicCacheService = publicCacheService;
    }

    public CourseContentServiceImpl(CourseContentRepository courseContentRepository,
                                    ModuleRepository moduleRepository,
                                    com.MOOC.OnlineLearningPlatfrom.Repository.StoredFileRepository storedFileRepository) {
        this(courseContentRepository, moduleRepository, storedFileRepository, null);
    }

    public CourseContentServiceImpl(CourseContentRepository courseContentRepository,
                                    ModuleRepository moduleRepository) {
        this(courseContentRepository, moduleRepository, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseContent getContentById(Long contentId) {
        return courseContentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + contentId));
    }

    private void invalidateCourseForModule(Module module) {
        if (publicCacheService != null && module != null && module.getCourse() != null) {
            publicCacheService.invalidateCourse(module.getCourse().getId());
        }
    }

    private void invalidateCourseForModuleId(Long moduleId) {
        if (publicCacheService != null && moduleId != null) {
            moduleRepository.findById(moduleId).ifPresent(this::invalidateCourseForModule);
        }
    }

    @Override
    public CourseContent addContentToModule(Long moduleId, CourseContent content) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));

        String type = content.getContentType() != null ? content.getContentType().toUpperCase() : null;
        if (type == null || (!type.equals("VIDEO") && !type.equals("PDF") && !type.equals("QUIZ"))) {
            throw new BadRequestException("contentType must be VIDEO, PDF, or QUIZ");
        }
        content.setContentType(type);
        content.setModule(module);

        if ("VIDEO".equals(type)) {
            validateAndPopulateVideoSource(content);
        }

        if (content.getPosition() == null || content.getPosition() <= 0) {
            List<CourseContent> existing = courseContentRepository.findByModule_ModuleIdOrderByPositionAsc(moduleId);
            content.setPosition(existing.size() + 1);
        }

        CourseContent saved = courseContentRepository.save(content);
        invalidateCourseForModule(module);
        return saved;
    }

    @Override
    public CourseContent updateContent(Long contentId, CourseContent updated) {
        CourseContent existing = getContentById(contentId);

        if (updated.getTitle() != null && !updated.getTitle().isBlank()) {
            existing.setTitle(updated.getTitle());
        }
        if (updated.getContentType() != null) {
            String type = updated.getContentType().toUpperCase();
            if (!type.equals("VIDEO") && !type.equals("PDF") && !type.equals("QUIZ")) {
                throw new BadRequestException("contentType must be VIDEO, PDF, or QUIZ");
            }
            existing.setContentType(type);
        }
        if (updated.getFilePath() != null) {
            existing.setFilePath(updated.getFilePath());
        }
        if (updated.getDurationSeconds() != null) {
            existing.setDurationSeconds(updated.getDurationSeconds());
        }
        if (updated.getSourceType() != null) {
            existing.setSourceType(updated.getSourceType());
        }
        if (updated.getStorageKey() != null) {
            existing.setStorageKey(updated.getStorageKey());
        }
        if (updated.getProviderVideoId() != null) {
            existing.setProviderVideoId(updated.getProviderVideoId());
        }
        if (updated.getExternalUrl() != null) {
            existing.setExternalUrl(updated.getExternalUrl());
        }
        if (updated.getMediaStatus() != null) {
            existing.setMediaStatus(updated.getMediaStatus());
        }
        if (updated.getMimeType() != null) {
            existing.setMimeType(updated.getMimeType());
        }
        if (updated.getSizeBytes() != null) {
            existing.setSizeBytes(updated.getSizeBytes());
        }
        if ("VIDEO".equalsIgnoreCase(existing.getContentType())) {
            validateAndPopulateVideoSource(existing);
        }
        if (updated.getPosition() != null && !updated.getPosition().equals(existing.getPosition())) {
            existing = courseContentRepository.save(existing);
            return reorderContent(contentId, new ReorderRequest(updated.getPosition(), null));
        }

        CourseContent saved = courseContentRepository.save(existing);
        invalidateCourseForModule(existing.getModule());
        return saved;
    }

    private void validateAndPopulateVideoSource(CourseContent content) {
        String sourceType = content.getSourceType();
        if (sourceType == null || sourceType.isBlank()) {
            if (content.getStorageKey() != null && !content.getStorageKey().isBlank()) {
                sourceType = "UPLOAD";
            } else if (content.getProviderVideoId() != null && !content.getProviderVideoId().isBlank()) {
                sourceType = "YOUTUBE";
            } else if (content.getExternalUrl() != null && !content.getExternalUrl().isBlank()) {
                sourceType = "EXTERNAL";
            } else if (content.getFilePath() != null && (content.getFilePath().contains("youtu.be") || content.getFilePath().contains("youtube.com"))) {
                sourceType = "YOUTUBE";
            } else {
                throw new BadRequestException("Video content must specify a valid source (UPLOAD, YOUTUBE, or EXTERNAL)");
            }
        }

        sourceType = sourceType.toUpperCase(java.util.Locale.ROOT);
        content.setSourceType(sourceType);

        if ("UPLOAD".equals(sourceType)) {
            if (content.getStorageKey() == null || content.getStorageKey().isBlank()) {
                throw new BadRequestException("storageKey is required for UPLOAD video");
            }
            if (storedFileRepository != null) {
                com.MOOC.OnlineLearningPlatfrom.Entity.StoredFile file = storedFileRepository.findByFileKey(content.getStorageKey())
                        .orElseThrow(() -> new BadRequestException("Storage file not found with key: " + content.getStorageKey()));
                if (content.getMimeType() == null || content.getMimeType().isBlank()) {
                    content.setMimeType(file.getContentType());
                }
                if (content.getSizeBytes() == null) {
                    content.setSizeBytes(file.getSizeBytes());
                }
            }
            if (content.getMediaStatus() == null || content.getMediaStatus().isBlank()) {
                content.setMediaStatus("READY");
            }
        } else if ("YOUTUBE".equals(sourceType)) {
            if (content.getProviderVideoId() == null || content.getProviderVideoId().isBlank()) {
                String vid = extractYoutubeId(content.getFilePath());
                if (vid == null || vid.isBlank()) {
                    vid = extractYoutubeId(content.getExternalUrl());
                }
                if (vid == null || vid.isBlank()) {
                    throw new BadRequestException("providerVideoId is required for YOUTUBE video");
                }
                content.setProviderVideoId(vid);
            }
            if (content.getMediaStatus() == null || content.getMediaStatus().isBlank()) {
                content.setMediaStatus("READY");
            }
        } else if ("EXTERNAL".equals(sourceType)) {
            if (content.getExternalUrl() == null || content.getExternalUrl().isBlank()
                    || (!content.getExternalUrl().startsWith("http://") && !content.getExternalUrl().startsWith("https://"))) {
                throw new BadRequestException("A valid externalUrl (http:// or https://) is required for EXTERNAL video");
            }
            if (content.getMediaStatus() == null || content.getMediaStatus().isBlank()) {
                content.setMediaStatus("READY");
            }
        } else {
            throw new BadRequestException("Unsupported video sourceType: " + sourceType);
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

    @Override
    public void deleteContent(Long contentId) {
        CourseContent content = getContentById(contentId);
        Module module = content.getModule();
        Long moduleId = module != null ? module.getModuleId() : null;
        courseContentRepository.delete(content);
        courseContentRepository.flush();

        // Re-normalise positions to avoid gaps (AC-08, [B-F6])
        if (moduleId != null) {
            List<CourseContent> remaining = courseContentRepository.findByModule_ModuleIdOrderByPositionAsc(moduleId);
            for (int i = 0; i < remaining.size(); i++) {
                remaining.get(i).setPosition(i + 1);
            }
            courseContentRepository.saveAll(remaining);
        }
        invalidateCourseForModule(module);
    }

    @Override
    public CourseContent reorderContent(Long contentId, ReorderRequest request) {
        CourseContent content = getContentById(contentId);
        Long moduleId = content.getModule() != null ? content.getModule().getModuleId() : null;
        if (moduleId == null) {
            return content;
        }

        if (request != null && request.getOrderedIds() != null && !request.getOrderedIds().isEmpty()) {
            reorderContents(moduleId, request);
            return getContentById(contentId);
        }

        int targetPosition = (request != null && request.getPosition() != null) ? request.getPosition() : 1;
        List<CourseContent> contents = courseContentRepository.findByModule_ModuleIdOrderByPositionAsc(moduleId);
        if (contents.isEmpty()) {
            return content;
        }

        targetPosition = Math.max(1, Math.min(targetPosition, contents.size()));

        int currentIndex = -1;
        for (int i = 0; i < contents.size(); i++) {
            if (contents.get(i).getContentId().equals(contentId)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex != -1) {
            CourseContent moved = contents.remove(currentIndex);
            int insertIndex = targetPosition - 1;
            contents.add(insertIndex, moved);

            for (int i = 0; i < contents.size(); i++) {
                contents.get(i).setPosition(i + 1);
            }
            courseContentRepository.saveAll(contents);
        }

        invalidateCourseForModule(content.getModule());
        return getContentById(contentId);
    }

    @Override
    public List<CourseContent> reorderContents(Long moduleId, ReorderRequest request) {
        List<CourseContent> contents = courseContentRepository.findByModule_ModuleIdOrderByPositionAsc(moduleId);
        if (request == null || request.getOrderedIds() == null || request.getOrderedIds().isEmpty()) {
            return contents;
        }

        List<Long> orderedIds = request.getOrderedIds();
        Map<Long, CourseContent> contentMap = contents.stream()
                .collect(Collectors.toMap(CourseContent::getContentId, c -> c));

        List<CourseContent> result = new ArrayList<>();
        for (Long id : orderedIds) {
            CourseContent c = contentMap.remove(id);
            if (c != null) {
                result.add(c);
            }
        }
        for (CourseContent c : contents) {
            if (contentMap.containsKey(c.getContentId())) {
                result.add(c);
            }
        }

        for (int i = 0; i < result.size(); i++) {
            result.get(i).setPosition(i + 1);
        }
        List<CourseContent> saved = courseContentRepository.saveAll(result);
        invalidateCourseForModuleId(moduleId);
        return saved;
    }
}
