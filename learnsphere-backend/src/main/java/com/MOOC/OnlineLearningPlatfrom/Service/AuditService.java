package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.AuditLogResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.PageResponse;
import com.MOOC.OnlineLearningPlatfrom.Entity.AuditLog;
import com.MOOC.OnlineLearningPlatfrom.Repository.AuditLogRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditService {

    public static final String USER_STATUS_CHANGED = "USER_STATUS_CHANGED";
    public static final String USER_ROLE_ASSIGNED = "USER_ROLE_ASSIGNED";
    public static final String USER_ROLE_REVOKED = "USER_ROLE_REVOKED";

    public static final String RESOURCE_USER = "USER";

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String action,
                       String resourceType,
                       Long resourceId,
                       Map<String, Object> before,
                       Map<String, Object> after) {
        AuditLog entry = new AuditLog();
        entry.setActorId(currentActorId());
        entry.setAction(action);
        entry.setResourceType(resourceType);
        entry.setResourceId(resourceId);
        entry.setBeforeJson(before);
        entry.setAfterJson(after);
        entry.setIp(currentIp());
        auditLogRepository.save(entry);
    }

    public PageResponse<AuditLogResponseDto> getAuditLogs(Long actorId,
                                                          String resourceType,
                                                          Long resourceId,
                                                          String action,
                                                          Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (actorId != null) {
                predicates.add(cb.equal(root.get("actorId"), actorId));
            }
            if (resourceType != null && !resourceType.isBlank()) {
                predicates.add(cb.equal(root.get("resourceType"), resourceType.trim()));
            }
            if (resourceId != null) {
                predicates.add(cb.equal(root.get("resourceId"), resourceId));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(AuditLogResponseDto::from));
    }

    /**
     * Builds a small, deterministic snapshot. Only explicitly supplied fields
     * are ever written, so a caller cannot accidentally persist a password or
     * token by passing an entity.
     */
    public static Map<String, Object> snapshot(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("snapshot requires key/value pairs");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            values.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return values;
    }

    private Long currentActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails details
                && details.getUser() != null) {
            return details.getUser().getUserId();
        }
        return null;
    }

    private String currentIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ip = forwardedFor != null && !forwardedFor.isBlank()
                ? forwardedFor.split(",")[0].trim()
                : request.getRemoteAddr();
        if (ip == null || ip.isBlank()) {
            return null;
        }
        return ip.length() <= 45 ? ip : ip.substring(0, 45);
    }
}
