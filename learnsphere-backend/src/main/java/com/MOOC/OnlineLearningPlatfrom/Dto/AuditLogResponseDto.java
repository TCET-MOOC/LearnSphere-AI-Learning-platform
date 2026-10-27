package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.AuditLog;

import java.time.LocalDateTime;
import java.util.Map;

public record AuditLogResponseDto(
        Long id,
        Long actorId,
        String action,
        String resourceType,
        Long resourceId,
        Map<String, Object> beforeJson,
        Map<String, Object> afterJson,
        String ip,
        LocalDateTime createdAt) {

    public static AuditLogResponseDto from(AuditLog log) {
        return new AuditLogResponseDto(
                log.getId(),
                log.getActorId(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getBeforeJson(),
                log.getAfterJson(),
                log.getIp(),
                log.getCreatedAt()
        );
    }
}
