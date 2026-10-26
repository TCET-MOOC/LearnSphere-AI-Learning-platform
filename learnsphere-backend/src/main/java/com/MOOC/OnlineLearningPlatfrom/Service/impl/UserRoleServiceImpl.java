package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.UserRoleDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Role;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserRole;
import com.MOOC.OnlineLearningPlatfrom.Exception.ConflictException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserRoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.AuditService;
import com.MOOC.OnlineLearningPlatfrom.Service.UserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;

    public UserRoleServiceImpl(UserRoleRepository userRoleRepository,
                               UserAccountRepository userAccountRepository,
                               RoleRepository roleRepository,
                               AuditService auditService) {
        this.userRoleRepository = userRoleRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRoleDto> getAllUserRoles() {
        return userRoleRepository.findAll().stream()
                .map(UserRoleDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserRoleDto getUserRoleById(Long id) {
        return UserRoleDto.from(findAssignment(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRoleDto> getRolesForUser(Long userId) {
        requireUser(userId);
        return userRoleRepository.findByUser_UserId(userId).stream()
                .map(UserRoleDto::from)
                .toList();
    }

    @Override
    @Transactional
    public UserRoleDto assignRole(Long userId, Long roleId) {
        UserAccount user = requireUser(userId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        if (userRoleRepository.findByUser_UserIdAndRole_RoleId(userId, roleId).isPresent()) {
            throw new ConflictException("User already has role " + role.getName());
        }

        UserRole assignment = new UserRole();
        assignment.setUser(user);
        assignment.setRole(role);
        UserRole saved = userRoleRepository.save(assignment);

        auditService.record(
                AuditService.USER_ROLE_ASSIGNED,
                AuditService.RESOURCE_USER,
                userId,
                null,
                roleSnapshot(user, role));
        return UserRoleDto.from(saved);
    }

    @Override
    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {
        UserRole assignment = userRoleRepository.findByUser_UserIdAndRole_RoleId(userId, roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role assignment not found"));
        auditService.record(
                AuditService.USER_ROLE_REVOKED,
                AuditService.RESOURCE_USER,
                userId,
                roleSnapshot(assignment.getUser(), assignment.getRole()),
                null);
        userRoleRepository.delete(assignment);
    }

    @Override
    @Transactional
    public void deleteUserRole(Long id) {
        UserRole assignment = findAssignment(id);
        UserAccount user = assignment.getUser();
        Role role = assignment.getRole();
        auditService.record(
                AuditService.USER_ROLE_REVOKED,
                AuditService.RESOURCE_USER,
                user != null ? user.getUserId() : null,
                roleSnapshot(user, role),
                null);
        userRoleRepository.delete(assignment);
    }

    private UserRole findAssignment(Long id) {
        return userRoleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User role assignment not found with id: " + id));
    }

    private UserAccount requireUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private static java.util.Map<String, Object> roleSnapshot(UserAccount user, Role role) {
        return AuditService.snapshot(
                "userId", user != null ? user.getUserId() : null,
                "roleId", role != null ? role.getRoleId() : null,
                "role", role != null ? role.getName() : null);
    }
}
