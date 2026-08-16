package com.MOOC.OnlineLearningPlatfrom.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public static class RegisterRequest {
        @NotBlank
        private String fullName;
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
        @NotBlank
        private String role; // STUDENT | TEACHER | ADMIN
        private Long collegeId;
        private Long departmentId;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Long getCollegeId() { return collegeId; }
        public void setCollegeId(Long collegeId) { this.collegeId = collegeId; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    }

    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class AuthResponse {
        private String token;
        private UserResponseDto user;

        public AuthResponse(String token, UserResponseDto user) {
            this.token = token;
            this.user = user;
        }

        public String getToken() { return token; }
        public UserResponseDto getUser() { return user; }
    }

    public static class ForgotPasswordRequest {
        @NotBlank @Email
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ResetPasswordRequest {
        @NotBlank
        private String token;
        @NotBlank
        private String newPassword;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class VerifyCollegeRequest {
        private Long collegeId;
        private String claimedDepartment;
        private String idDocumentUrl;

        public Long getCollegeId() { return collegeId; }
        public void setCollegeId(Long collegeId) { this.collegeId = collegeId; }
        public String getClaimedDepartment() { return claimedDepartment; }
        public void setClaimedDepartment(String claimedDepartment) { this.claimedDepartment = claimedDepartment; }
        public String getIdDocumentUrl() { return idDocumentUrl; }
        public void setIdDocumentUrl(String idDocumentUrl) { this.idDocumentUrl = idDocumentUrl; }
    }
}
