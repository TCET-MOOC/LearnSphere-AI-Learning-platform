package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import org.springframework.stereotype.Service;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.DepartmentService;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository,
                                 CourseRepository courseRepository) {
        this.departmentRepository = departmentRepository;
        this.courseRepository = courseRepository;
    }


    @Override
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Override
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));
    }

    @Override
    public Department createDepartment(Department department) {
        return departmentRepository.save(department);
    }

    @Override
    public Department updateDepartment(Long id, Department department) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));
        existing.setName(department.getName());
        return departmentRepository.save(existing);
    }

    @Override
    public void deleteDepartment(Long id) {
        departmentRepository.deleteById(id);
    }

    @Override
    public List<Course> getCoursesByDepartmentId(Long id) {
        Department dept = getDepartmentById(id);
        return courseRepository.findByDepartment(dept.getName());
    }
}

