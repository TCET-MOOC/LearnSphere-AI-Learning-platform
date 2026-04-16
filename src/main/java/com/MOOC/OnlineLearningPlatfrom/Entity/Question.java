package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    // We removed: private Long testId;

    @Column(columnDefinition = "TEXT")
    private String body;
    private String questionType;
    private Integer marks;

    // --- NEW RELATIONSHIP ---
    // Many questions can belong to one test.
    @ManyToOne
    @JoinColumn(name = "test_id") // This creates a 'test_id' foreign key column
    @JsonIgnore // Prevents infinite loops when sending data as JSON
    private Test test;


    // --- Getters and Setters for all fields, including the new Test object ---

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public Integer getMarks() {
        return marks;
    }

    public void setMarks(Integer marks) {
        this.marks = marks;
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }
}