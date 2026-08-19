package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.AnnouncementRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.AnnouncementResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;

import java.util.List;

public interface AnnouncementService {
    List<AnnouncementResponseDto> getAnnouncements(CustomUserDetails principal);
    AnnouncementResponseDto createAnnouncement(AnnouncementRequestDto request, CustomUserDetails principal);
    AnnouncementResponseDto updateAnnouncement(Long id, AnnouncementRequestDto request, CustomUserDetails principal);
    void deleteAnnouncement(Long id, CustomUserDetails principal);
    AnnouncementResponseDto togglePin(Long id, CustomUserDetails principal);
}
