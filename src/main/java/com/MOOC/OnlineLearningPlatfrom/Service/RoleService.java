package com.MOOC.OnlineLearningPlatfrom.Service;

import java.util.List;

import com.MOOC.OnlineLearningPlatfrom.Entity.Role;

public interface RoleService {
    List<Role> getAllRoles();
    Role getRoleById(Long id);
    Role createRole(Role role);
    Role updateRole(Long id, Role role);
    void deleteRole(Long id);
}
