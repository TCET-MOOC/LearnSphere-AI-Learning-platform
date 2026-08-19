package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.DiscussionPostDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.DiscussionPostRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;

import java.util.List;

public interface DiscussionService {
    List<DiscussionPostDto> getCourseDiscussion(Long courseId);
    List<DiscussionPostDto> getLectureDiscussion(Long lectureId);
    DiscussionPostDto createPost(DiscussionPostRequestDto request, CustomUserDetails principal);
    void deletePost(Long id, CustomUserDetails principal);
}
