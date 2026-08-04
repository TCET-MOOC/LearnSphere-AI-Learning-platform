package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTeacher_UserId(Long teacherId);
    List<Course> findByStatus(Course.Status status);
    List<Course> findByDepartment(String department);
    List<Course> findByStatusAndDepartment(Course.Status status, String department);
    long countByTeacher_College_Id(Long collegeId);
}
