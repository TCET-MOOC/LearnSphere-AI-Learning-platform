package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import org.springframework.stereotype.Service;

import com.MOOC.OnlineLearningPlatfrom.Entity.UserRole;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserRoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.UserRoleService;

import java.util.List;

@Service
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleRepository userRoleRepository;

    public UserRoleServiceImpl(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public List<UserRole> getAllUserRoles() {
        return userRoleRepository.findAll();
    }

    @Override
    public UserRole getUserRoleById(Long id) {
        return userRoleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("UserRole not found with id: " + id));
    }

    @Override
    public UserRole createUserRole(UserRole userRole) {
        return userRoleRepository.save(userRole);
    }

    @Override
    public void deleteUserRole(Long id) {
        userRoleRepository.deleteById(id);
    }
}
