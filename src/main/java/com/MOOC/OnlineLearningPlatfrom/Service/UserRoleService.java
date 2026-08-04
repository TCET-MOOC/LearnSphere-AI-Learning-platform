package com.MOOC.OnlineLearningPlatfrom.Service;

import java.util.List;

import com.MOOC.OnlineLearningPlatfrom.Entity.UserRole;

public interface UserRoleService {
    List<UserRole> getAllUserRoles();
    UserRole getUserRoleById(Long id);
    UserRole createUserRole(UserRole userRole);
    void deleteUserRole(Long id);
}
