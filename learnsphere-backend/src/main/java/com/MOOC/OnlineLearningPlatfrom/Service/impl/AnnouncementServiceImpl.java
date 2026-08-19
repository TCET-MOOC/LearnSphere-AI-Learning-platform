package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.AnnouncementRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.AnnouncementResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Announcement;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.AnnouncementRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.AnnouncementService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementServiceImpl(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @Override
    public List<AnnouncementResponseDto> getAnnouncements(CustomUserDetails principal) {
        Announcement.Audience audience = resolveAudience(principal);
        List<Announcement.Audience> audiences = audience == Announcement.Audience.ALL
                ? List.of(Announcement.Audience.ALL)
                : List.of(Announcement.Audience.ALL, audience);
        return announcementRepository.findByAudienceInOrderByPinnedDescCreatedAtDesc(audiences).stream()
                .map(AnnouncementResponseDto::from)
                .toList();
    }

    @Override
    public AnnouncementResponseDto createAnnouncement(AnnouncementRequestDto request, CustomUserDetails principal) {
        requireStaff(principal);
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("title is required");
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setBody(request.getBody());
        announcement.setCategory(request.getCategory());
        announcement.setAuthor(principal.getUser());
        announcement.setPinned(Boolean.TRUE.equals(request.getPinned()));
        announcement.setAudience(parseAudience(request.getAudience()));
        return AnnouncementResponseDto.from(announcementRepository.save(announcement));
    }

    @Override
    public AnnouncementResponseDto updateAnnouncement(Long id, AnnouncementRequestDto request, CustomUserDetails principal) {
        Announcement announcement = findOrThrow(id);
        requireAuthorOrAdmin(announcement, principal);

        if (request.getTitle() != null) announcement.setTitle(request.getTitle());
        if (request.getBody() != null) announcement.setBody(request.getBody());
        if (request.getCategory() != null) announcement.setCategory(request.getCategory());
        if (request.getAudience() != null) announcement.setAudience(parseAudience(request.getAudience()));
        if (request.getPinned() != null) announcement.setPinned(request.getPinned());

        return AnnouncementResponseDto.from(announcementRepository.save(announcement));
    }

    @Override
    public void deleteAnnouncement(Long id, CustomUserDetails principal) {
        Announcement announcement = findOrThrow(id);
        requireAuthorOrAdmin(announcement, principal);
        announcementRepository.delete(announcement);
    }

    @Override
    public AnnouncementResponseDto togglePin(Long id, CustomUserDetails principal) {
        if (!principal.getRoles().contains("ADMIN")) {
            throw new AccessDeniedException("Only admins can pin announcements");
        }
        Announcement announcement = findOrThrow(id);
        announcement.setPinned(!Boolean.TRUE.equals(announcement.getPinned()));
        return AnnouncementResponseDto.from(announcementRepository.save(announcement));
    }

    private Announcement findOrThrow(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));
    }

    private void requireStaff(CustomUserDetails principal) {
        if (!principal.getRoles().contains("ADMIN") && !principal.getRoles().contains("TEACHER")) {
            throw new AccessDeniedException("Only teachers or admins can create announcements");
        }
    }

    private void requireAuthorOrAdmin(Announcement announcement, CustomUserDetails principal) {
        boolean isAdmin = principal.getRoles().contains("ADMIN");
        boolean isAuthor = announcement.getAuthor() != null
                && announcement.getAuthor().getUserId().equals(principal.getUser().getUserId());
        if (!isAdmin && !isAuthor) {
            throw new AccessDeniedException("Only the author or an admin can modify this announcement");
        }
    }

    private Announcement.Audience resolveAudience(CustomUserDetails principal) {
        List<String> roles = principal.getRoles();
        if (roles.contains("ADMIN")) return Announcement.Audience.ADMIN;
        if (roles.contains("TEACHER")) return Announcement.Audience.TEACHER;
        if (roles.contains("STUDENT")) return Announcement.Audience.STUDENT;
        return Announcement.Audience.ALL;
    }

    private Announcement.Audience parseAudience(String value) {
        if (value == null || value.isBlank()) return Announcement.Audience.ALL;
        try {
            return Announcement.Audience.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid audience: " + value + ". Must be one of "
                    + Arrays.toString(Announcement.Audience.values()));
        }
    }
}
