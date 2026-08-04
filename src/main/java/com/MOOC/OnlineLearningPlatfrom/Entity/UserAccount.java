package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String fullName;

    @Column(unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @ManyToOne
    @JoinColumn(name = "dept_id")
    private Department department;

    @ManyToOne
    @JoinColumn(name = "college_id")
    private College college;

    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    private Boolean twoFactorEnabled = false;
    private Boolean bankAccountLinked = false;
    private BigDecimal royaltyBalance = BigDecimal.ZERO;
    private Integer attendanceScore = 100;
    private Integer leaderboardPoints = 0;

    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
    }

    public enum Status {
        ACTIVE, FLAGGED, BLACKLISTED
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public College getCollege() { return college; }
    public void setCollege(College college) { this.college = college; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(Boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }
    public Boolean isBankAccountLinked() { return bankAccountLinked; }
    public void setBankAccountLinked(Boolean bankAccountLinked) { this.bankAccountLinked = bankAccountLinked; }
    public BigDecimal getRoyaltyBalance() { return royaltyBalance; }
    public void setRoyaltyBalance(BigDecimal royaltyBalance) { this.royaltyBalance = royaltyBalance; }
    public Integer getAttendanceScore() { return attendanceScore; }
    public void setAttendanceScore(Integer attendanceScore) { this.attendanceScore = attendanceScore; }
    public Integer getLeaderboardPoints() { return leaderboardPoints; }
    public void setLeaderboardPoints(Integer leaderboardPoints) { this.leaderboardPoints = leaderboardPoints; }
    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
