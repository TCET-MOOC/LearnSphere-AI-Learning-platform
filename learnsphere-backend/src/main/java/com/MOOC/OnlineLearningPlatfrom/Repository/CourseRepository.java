package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findFirstByTitle(String title);
    List<Course> findByTeacher_UserId(Long teacherId);
    List<Course> findByStatus(Course.Status status);
    List<Course> findByDepartment(Department department);
    List<Course> findByDepartment_DepartmentId(Long departmentId);
    List<Course> findByStatusAndDepartment_DepartmentId(Course.Status status, Long departmentId);
    List<Course> findByDepartmentLabel(String departmentLabel);
    List<Course> findByStatusAndDepartmentLabel(Course.Status status, String departmentLabel);
    long countByTeacher_College_Id(Long collegeId);
    long countByTeacher_UserId(Long teacherId);
}
