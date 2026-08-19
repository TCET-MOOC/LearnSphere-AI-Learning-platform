package com.MOOC.OnlineLearningPlatfrom.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_roles")
public class UserRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userRoleId;

    @ManyToOne
    @JoinColumn(name = "user_id") // <-- SIMPLIFIED HERE
    private UserAccount user;

    @ManyToOne
    @JoinColumn(name = "role_id") // <-- AND SIMPLIFIED HERE
    private Role role;

    // --- Getters and Setters ---
    public Long getUserRoleId() { return userRoleId; }
    public void setUserRoleId(Long userRoleId) { this.userRoleId = userRoleId; }
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}