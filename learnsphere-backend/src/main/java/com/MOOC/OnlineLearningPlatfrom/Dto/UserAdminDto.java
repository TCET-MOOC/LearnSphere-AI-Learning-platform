package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Flattened view of a UserAccount for the admin "User Management" screen:
 * combines account fields with its role(s) and college/department names.
 */
public class UserAdminDto {
    private Long id;
    private String name;
    private String email;
    private String role;
    private String department;
    private String college;
    private String status;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;

    public static UserAdminDto from(UserAccount user, List<String> roles) {
        UserAdminDto dto = new UserAdminDto();
        dto.id = user.getUserId();
        dto.name = user.getFullName();
        dto.email = user.getEmail();
        dto.role = roles == null || roles.isEmpty() ? "UNASSIGNED" : String.join(", ", roles);
        dto.department = user.getDepartment() != null ? user.getDepartment().getName() : null;
        dto.college = user.getCollege() != null ? user.getCollege().getName() : null;
        dto.status = user.getStatus() != null ? user.getStatus().name() : null;
        dto.lastActiveAt = user.getLastActiveAt();
        dto.createdAt = user.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getDepartment() { return department; }
    public String getCollege() { return college; }
    public String getStatus() { return status; }
    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
