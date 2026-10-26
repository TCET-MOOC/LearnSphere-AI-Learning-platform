package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.ModuleRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.CourseContentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        if (content.getPosition() == null || content.getPosition() == 0) {
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
        if (updated.getPosition() != null) {
            existing.setPosition(updated.getPosition());
        }

        return courseContentRepository.save(existing);
    }

    @Override
    public void deleteContent(Long contentId) {
        CourseContent content = getContentById(contentId);
        courseContentRepository.delete(content);
    }
}
