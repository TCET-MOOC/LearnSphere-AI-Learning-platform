package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.fasterxml.jackson.annotation.JsonProperty; // <-- IMPORTANT: New import
import jakarta.persistence.*;

@Entity
@Table(name = "student_answers")
public class StudentAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studentAnswerId;

    @Column(columnDefinition = "TEXT")
    private String answerText;
    private Integer marksAwarded;

    // --- UPDATED RELATIONSHIPS ---

    @ManyToOne
    @JoinColumn(name = "attempt_id")
    // This allows you to SEND an attempt ID, but it won't be in the RESPONSE JSON
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private TestAttempt attempt;

    @ManyToOne
    @JoinColumn(name = "question_id")
    // This allows you to SEND a question ID, but it won't be in the RESPONSE JSON
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Question question;


    // --- Getters and Setters ---

    public Long getStudentAnswerId() {
        return studentAnswerId;
    }

    public void setStudentAnswerId(Long studentAnswerId) {
        this.studentAnswerId = studentAnswerId;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public Integer getMarksAwarded() {
        return marksAwarded;
    }

    public void setMarksAwarded(Integer marksAwarded) {
        this.marksAwarded = marksAwarded;
    }

    public TestAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(TestAttempt attempt) {
        this.attempt = attempt;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }
}