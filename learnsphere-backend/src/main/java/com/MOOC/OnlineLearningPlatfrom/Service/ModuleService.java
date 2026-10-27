package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;

import java.util.List;

public interface ModuleService {
    List<Module> getModulesByCourseId(Long courseId);
    Module getModuleById(Long moduleId);
    Module createModule(Long courseId, Module module);
    Module updateModule(Long moduleId, Module module);
    void deleteModule(Long moduleId);
    List<CourseContent> getContentsByModuleId(Long moduleId);
    Module reorderModule(Long moduleId, com.MOOC.OnlineLearningPlatfrom.dto.ReorderRequest request);
    List<Module> reorderModules(Long courseId, com.MOOC.OnlineLearningPlatfrom.dto.ReorderRequest request);
}
