package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.College;

import java.time.LocalDateTime;

public class CollegeResponseDto {
    private Long id;
    private String name;
    private String city;
    private long studentCount;
    private long teacherCount;
    private long courseCount;
    private String verificationStatus;
    private LocalDateTime appliedAt;

    public static CollegeResponseDto from(College college, long studentCount, long teacherCount, long courseCount) {
        CollegeResponseDto dto = new CollegeResponseDto();
        dto.id = college.getId();
        dto.name = college.getName();
        dto.city = college.getCity();
        dto.studentCount = studentCount;
        dto.teacherCount = teacherCount;
        dto.courseCount = courseCount;
        dto.verificationStatus = college.getVerificationStatus() != null ? college.getVerificationStatus().name() : null;
        dto.appliedAt = college.getAppliedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public long getStudentCount() { return studentCount; }
    public long getTeacherCount() { return teacherCount; }
    public long getCourseCount() { return courseCount; }
    public String getVerificationStatus() { return verificationStatus; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
}
