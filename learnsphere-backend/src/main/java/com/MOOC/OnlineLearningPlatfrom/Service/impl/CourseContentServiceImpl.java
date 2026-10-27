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

    public CourseContentServiceImpl(CourseContentRepository courseContentRepository,
                                    ModuleRepository moduleRepository) {
        this.courseContentRepository = courseContentRepository;
        this.moduleRepository = moduleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CourseContent getContentById(Long contentId) {
        return courseContentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + contentId));
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

        if (content.getPosition() == null || content.getPosition() <= 0) {
            List<CourseContent> existing = courseContentRepository.findByModule_ModuleIdOrderByPositionAsc(moduleId);
            content.setPosition(existing.size() + 1);
        }

        return courseContentRepository.save(content);
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
        if (updated.getPosition() != null && !updated.getPosition().equals(existing.getPosition())) {
            existing = courseContentRepository.save(existing);
            return reorderContent(contentId, new ReorderRequest(updated.getPosition(), null));
        }

        return courseContentRepository.save(existing);
    }

    @Override
    public void deleteContent(Long contentId) {
        CourseContent content = getContentById(contentId);
        Long moduleId = content.getModule() != null ? content.getModule().getModuleId() : null;
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
        return courseContentRepository.saveAll(result);
    }
}
