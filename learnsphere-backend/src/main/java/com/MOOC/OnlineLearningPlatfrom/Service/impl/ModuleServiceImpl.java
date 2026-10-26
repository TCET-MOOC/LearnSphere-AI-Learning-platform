package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.ModuleRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.ModuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ModuleServiceImpl implements ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;
    private final CourseContentRepository courseContentRepository;

    public ModuleServiceImpl(ModuleRepository moduleRepository,
                             CourseRepository courseRepository,
                             CourseContentRepository courseContentRepository) {
        this.moduleRepository = moduleRepository;
        this.courseRepository = courseRepository;
        this.courseContentRepository = courseContentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Module> getModulesByCourseId(Long courseId) {
        return moduleRepository.findByCourse_IdOrderByPositionAsc(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Module getModuleById(Long moduleId) {
        return moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));
    }

    @Override
    public Module createModule(Long courseId, Module module) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        module.setCourse(course);
        if (module.getPosition() == null || module.getPosition() == 0) {
            List<Module> existing = moduleRepository.findByCourse_Id(courseId);
            module.setPosition(existing.size() + 1);
        }
        return moduleRepository.save(module);
    }

    @Override
    public Module updateModule(Long moduleId, Module updatedModule) {
        Module existing = getModuleById(moduleId);
        if (updatedModule.getTitle() != null && !updatedModule.getTitle().isBlank()) {
            existing.setTitle(updatedModule.getTitle());
        }
        if (updatedModule.getPosition() != null) {
            existing.setPosition(updatedModule.getPosition());
        }
        return moduleRepository.save(existing);
    }

    @Override
    public void deleteModule(Long moduleId) {
        Module module = getModuleById(moduleId);
        Long courseId = module.getCourse() != null ? module.getCourse().getId() : null;
        moduleRepository.delete(module);

        // Re-normalise positions to avoid gaps
        if (courseId != null) {
            List<Module> remaining = moduleRepository.findByCourse_IdOrderByPositionAsc(courseId);
            for (int i = 0; i < remaining.size(); i++) {
                remaining.get(i).setPosition(i + 1);
            }
            moduleRepository.saveAll(remaining);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseContent> getContentsByModuleId(Long moduleId) {
        // verify module exists
        getModuleById(moduleId);
        return courseContentRepository.findByModule_ModuleIdOrderByPositionAsc(moduleId);
    }
}
