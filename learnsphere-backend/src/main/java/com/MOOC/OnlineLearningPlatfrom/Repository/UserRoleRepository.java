package com.MOOC.OnlineLearningPlatfrom.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.UserRole;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUser_UserId(Long userId);
    Optional<UserRole> findByUser_UserIdAndRole_RoleId(Long userId, Long roleId);
    void deleteByUser_UserIdAndRole_RoleId(Long userId, Long roleId);
    long countByRole_NameAndUser_College_Id(String roleName, Long collegeId);
    long countByRole_Name(String roleName);
}

