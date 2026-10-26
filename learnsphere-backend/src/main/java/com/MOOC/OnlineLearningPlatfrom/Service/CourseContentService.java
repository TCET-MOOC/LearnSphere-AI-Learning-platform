package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;

public interface CourseContentService {
    CourseContent getContentById(Long contentId);
    CourseContent addContentToModule(Long moduleId, CourseContent content);
    CourseContent updateContent(Long contentId, CourseContent content);
    void deleteContent(Long contentId);
}
