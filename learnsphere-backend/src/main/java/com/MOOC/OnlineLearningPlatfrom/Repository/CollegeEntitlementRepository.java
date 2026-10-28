package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.CollegeEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollegeEntitlementRepository extends JpaRepository<CollegeEntitlement, Long> {

    Optional<CollegeEntitlement> findByStudent_UserIdAndCourse_Id(Long studentId, Long courseId);

    boolean existsByStudent_UserIdAndCourse_IdAndRevokedFalse(Long studentId, Long courseId);

    List<CollegeEntitlement> findByStudent_UserId(Long studentId);

    List<CollegeEntitlement> findByCourse_Id(Long courseId);
}
