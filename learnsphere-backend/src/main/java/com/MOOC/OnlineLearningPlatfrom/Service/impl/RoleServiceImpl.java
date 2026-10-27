package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.Role;
import com.MOOC.OnlineLearningPlatfrom.Entity.RoleVocabulary;
import com.MOOC.OnlineLearningPlatfrom.Exception.ConflictException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
    }

    @Override
    @Transactional
    public Role createRole(Role role) {
        String canonicalName = RoleVocabulary.canonicalize(role != null ? role.getName() : null);
        Role existing = roleRepository.findByName(canonicalName);
        if (existing != null) {
            throw new ConflictException("Role already exists: " + canonicalName);
        }
        Role newRole = new Role();
        newRole.setName(canonicalName);
        return roleRepository.save(newRole);
    }

    @Override
    @Transactional
    public Role updateRole(Long id, Role role) {
        Role existing = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        String canonicalName = RoleVocabulary.canonicalize(role != null ? role.getName() : null);
        Role withSameName = roleRepository.findByName(canonicalName);
        if (withSameName != null && !withSameName.getRoleId().equals(id)) {
            throw new ConflictException("Role already exists: " + canonicalName);
        }
        existing.setName(canonicalName);
        return roleRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role existing = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        if (RoleVocabulary.CANONICAL_ROLES.contains(existing.getName())) {
            throw new ConflictException("Cannot delete reference role: " + existing.getName());
        }
        roleRepository.delete(existing);
    }
}
