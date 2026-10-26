package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.UserRoleDto;

import java.util.List;

public interface UserRoleService {

    List<UserRoleDto> getAllUserRoles();

    UserRoleDto getUserRoleById(Long id);

    List<UserRoleDto> getRolesForUser(Long userId);

    UserRoleDto assignRole(Long userId, Long roleId);

    void removeRoleFromUser(Long userId, Long roleId);

    void deleteUserRole(Long id);
}
