package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.PageResponse;
import com.MOOC.OnlineLearningPlatfrom.Dto.TopTeacherDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserActivitySummaryDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserAdminDetailDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserAdminDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserCountsDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserAdminService {
    PageResponse<UserAdminDto> getUsers(String role, String status, String search, Pageable pageable);
    UserCountsDto getCounts();
    UserAdminDetailDto getUserDetail(Long userId);
    UserAdminDto updateStatus(Long userId, String status);
    UserActivitySummaryDto getActivitySummary();
    List<TopTeacherDto> getTopTeachers(int limit);
}
