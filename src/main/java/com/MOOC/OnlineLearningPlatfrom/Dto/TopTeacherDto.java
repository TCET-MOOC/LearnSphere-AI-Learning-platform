package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.math.BigDecimal;

/** Real teacher ranking for the admin dashboard: students taught (via enrollments) + royalty earnings. */
public class TopTeacherDto {
    private Long id;
    private String name;
    private long students;
    private BigDecimal earnings;

    public TopTeacherDto(Long id, String name, long students, BigDecimal earnings) {
        this.id = id;
        this.name = name;
        this.students = students;
        this.earnings = earnings;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public long getStudents() { return students; }
    public BigDecimal getEarnings() { return earnings; }
}
