package com.MOOC.OnlineLearningPlatfrom.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lectures")
@Data
@NoArgsConstructor
public class Lecture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    private String title;

    private Integer number;

    private String videoUrl;

    private Integer duration;

    private Boolean isDownloadable = false;

    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;

    public enum Status {
        PUBLISHED, DRAFT, PROCESSING
    }
}
