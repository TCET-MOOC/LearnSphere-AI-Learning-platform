package com.MOOC.OnlineLearningPlatfrom.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.UserRole;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUser_UserId(Long userId);
    long countByRole_NameAndUser_College_Id(String roleName, Long collegeId);
    long countByRole_Name(String roleName);
}
