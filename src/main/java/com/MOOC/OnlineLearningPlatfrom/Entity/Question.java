package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.List;

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

    // Options for MCQ-type questions (nullable for non-MCQ types)
    @ElementCollection
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text")
    private List<String> options;

    // The correct option text (for MCQ) or a short-answer key. Never exposed to students.
    private String correctAnswer;

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

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
}