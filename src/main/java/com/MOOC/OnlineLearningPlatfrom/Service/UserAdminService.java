package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.TopTeacherDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserActivitySummaryDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserAdminDto;

import java.util.List;

public interface UserAdminService {
    List<UserAdminDto> getUsers(String role, String status, String search);
    UserAdminDto updateStatus(Long userId, String status);
    UserActivitySummaryDto getActivitySummary();
    List<TopTeacherDto> getTopTeachers(int limit);
}
