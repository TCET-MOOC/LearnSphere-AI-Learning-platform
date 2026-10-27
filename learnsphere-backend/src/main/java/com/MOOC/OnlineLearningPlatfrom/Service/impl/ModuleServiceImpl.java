package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.ModuleRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.ModuleService;
import com.MOOC.OnlineLearningPlatfrom.dto.ReorderRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if (module.getPosition() == null || module.getPosition() <= 0) {
            List<Module> existing = moduleRepository.findByCourse_IdOrderByPositionAsc(courseId);
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
        if (updatedModule.getPosition() != null && !updatedModule.getPosition().equals(existing.getPosition())) {
            existing = moduleRepository.save(existing);
            return reorderModule(moduleId, new ReorderRequest(updatedModule.getPosition(), null));
        }
        return moduleRepository.save(existing);
    }

    @Override
    public void deleteModule(Long moduleId) {
        Module module = getModuleById(moduleId);
        Long courseId = module.getCourse() != null ? module.getCourse().getId() : null;
        moduleRepository.delete(module);
        moduleRepository.flush();

        // Re-normalise positions to avoid gaps (AC-08)
        if (courseId != null) {
            List<Module> remaining = moduleRepository.findByCourse_IdOrderByPositionAsc(courseId);
            for (int i = 0; i < remaining.size(); i++) {
                remaining.get(i).setPosition(i + 1);
            }
            moduleRepository.saveAll(remaining);
        }
    }

    @Override
    public Module reorderModule(Long moduleId, ReorderRequest request) {
        Module module = getModuleById(moduleId);
        Long courseId = module.getCourse() != null ? module.getCourse().getId() : null;
        if (courseId == null) {
            return module;
        }

        if (request != null && request.getOrderedIds() != null && !request.getOrderedIds().isEmpty()) {
            reorderModules(courseId, request);
            return getModuleById(moduleId);
        }

        int targetPosition = (request != null && request.getPosition() != null) ? request.getPosition() : 1;
        List<Module> modules = moduleRepository.findByCourse_IdOrderByPositionAsc(courseId);
        if (modules.isEmpty()) {
            return module;
        }

        // Clamp target position to 1..modules.size()
        targetPosition = Math.max(1, Math.min(targetPosition, modules.size()));

        int currentIndex = -1;
        for (int i = 0; i < modules.size(); i++) {
            if (modules.get(i).getModuleId().equals(moduleId)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex != -1) {
            Module moved = modules.remove(currentIndex);
            int insertIndex = targetPosition - 1;
            modules.add(insertIndex, moved);

            // Re-normalize positions 1..n
            for (int i = 0; i < modules.size(); i++) {
                modules.get(i).setPosition(i + 1);
            }
            moduleRepository.saveAll(modules);
        }

        return getModuleById(moduleId);
    }

    @Override
    public List<Module> reorderModules(Long courseId, ReorderRequest request) {
        List<Module> modules = moduleRepository.findByCourse_IdOrderByPositionAsc(courseId);
        if (request == null || request.getOrderedIds() == null || request.getOrderedIds().isEmpty()) {
            return modules;
        }

        List<Long> orderedIds = request.getOrderedIds();
        Map<Long, Module> moduleMap = modules.stream()
                .collect(Collectors.toMap(Module::getModuleId, m -> m));

        List<Module> result = new ArrayList<>();
        for (Long id : orderedIds) {
            Module m = moduleMap.remove(id);
            if (m != null) {
                result.add(m);
            }
        }
        for (Module m : modules) {
            if (moduleMap.containsKey(m.getModuleId())) {
                result.add(m);
            }
        }

        for (int i = 0; i < result.size(); i++) {
            result.get(i).setPosition(i + 1);
        }
        return moduleRepository.saveAll(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseContent> getContentsByModuleId(Long moduleId) {
        // verify module exists
        getModuleById(moduleId);
        return courseContentRepository.findByModule_ModuleIdOrderByPositionAsc(moduleId);
    }
}
