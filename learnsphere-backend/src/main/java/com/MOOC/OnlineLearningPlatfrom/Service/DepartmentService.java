package com.MOOC.OnlineLearningPlatfrom.Service;

import java.util.List;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;

public interface DepartmentService {
    List<Department> getAllDepartments();
    Department getDepartmentById(Long id);
    Department createDepartment(Department department);
    Department updateDepartment(Long id, Department department);
    void deleteDepartment(Long id);
    List<Course> getCoursesByDepartmentId(Long id);
}


