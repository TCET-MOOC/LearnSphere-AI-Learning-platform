package com.MOOC.OnlineLearningPlatfrom.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    // Example of a custom query
    Department findByName(String name);
}