package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Question entity representing assessment items.
 * Per LMS Entity Reference Section 7 and A-12:
 * - No separate question_options table.
 * - Options are stored in Question.extra as a JSON array.
 * - For MCQ, correctAnswer must match one of the options.
 * - correctAnswer is never exposed to students.
 */
@Entity
@Table(name = "questions")
public class Question {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @Column(columnDefinition = "TEXT")
    private String body;

    private String questionType;

    private BigDecimal marks;

    // The correct option text (for MCQ) or a short-answer key. Never exposed to students.
    private String correctAnswer;

    @Column(name = "extra", columnDefinition = "TEXT")
    private String extra;

    @Transient
    private List<String> options;

    @ManyToOne
    @JoinColumn(name = "test_id")
    @JsonIgnore
    private Test test;

    public Question() {}

    @PostLoad
    public void onLoad() {
        if (this.extra != null && !this.extra.trim().isEmpty()) {
            this.options = parseOptionsFromExtra(this.extra);
        }
    }

    @PrePersist
    @PreUpdate
    public void onPersistOrUpdate() {
        if ((this.extra == null || this.extra.trim().isEmpty()) && this.options != null && !this.options.isEmpty()) {
            try {
                this.extra = OBJECT_MAPPER.writeValueAsString(this.options);
            } catch (Exception e) {
                this.extra = "[]";
            }
        }
        if ((this.extra == null || this.extra.trim().isEmpty()) && (this.options == null || this.options.isEmpty())) {
            if (this.correctAnswer != null && !this.correctAnswer.trim().isEmpty()) {
                this.options = List.of(this.correctAnswer);
                try {
                    this.extra = OBJECT_MAPPER.writeValueAsString(this.options);
                } catch (Exception e) {
                    this.extra = "[]";
                }
            }
        }
        validateMcqCorrectAnswer();
    }

    public void validateMcqCorrectAnswer() {
        if (isMcq()) {
            List<String> opts = getOptions();
            if (opts == null || opts.isEmpty()) {
                throw new BadRequestException("MCQ questions must specify options in extra.");
            }
            if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
                throw new BadRequestException("MCQ questions must specify a correctAnswer.");
            }
            boolean matched = opts.stream()
                    .anyMatch(opt -> opt != null && opt.trim().equalsIgnoreCase(correctAnswer.trim()));
            if (!matched) {
                throw new BadRequestException("For MCQ questions, correctAnswer must match one of the options in extra.");
            }
        }
    }

    public boolean isMcq() {
        if (questionType == null) return false;
        String t = questionType.trim().toUpperCase();
        return t.equals("MCQ") || t.equals("MULTIPLE_CHOICE");
    }

    private static List<String> parseOptionsFromExtra(String extraStr) {
        if (extraStr == null || extraStr.trim().isEmpty()) {
            return List.of();
        }
        try {
            String trimmed = extraStr.trim();
            if (trimmed.startsWith("[")) {
                return OBJECT_MAPPER.readValue(trimmed, new TypeReference<List<String>>() {});
            } else if (trimmed.startsWith("{")) {
                JsonNode root = OBJECT_MAPPER.readTree(trimmed);
                if (root.has("options") && root.get("options").isArray()) {
                    return OBJECT_MAPPER.convertValue(root.get("options"), new TypeReference<List<String>>() {});
                }
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    // --- Getters & Setters ---

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

    public BigDecimal getMarks() {
        return marks;
    }

    public void setMarks(BigDecimal marks) {
        this.marks = marks;
    }

    public void setMarks(int marks) {
        this.marks = BigDecimal.valueOf(marks);
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public List<String> getOptions() {
        if (this.options == null && this.extra != null && !this.extra.trim().isEmpty()) {
            this.options = parseOptionsFromExtra(this.extra);
        }
        return this.options;
    }

    public void setOptions(List<String> options) {
        this.options = options != null ? new ArrayList<>(options) : null;
        if (this.options != null && !this.options.isEmpty()) {
            try {
                this.extra = OBJECT_MAPPER.writeValueAsString(this.options);
            } catch (Exception e) {
                this.extra = "[]";
            }
        } else if (this.options != null) {
            this.extra = "[]";
        }
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
        if (extra != null && !extra.trim().isEmpty()) {
            List<String> parsed = parseOptionsFromExtra(extra);
            if (!parsed.isEmpty()) {
                this.options = parsed;
            }
        }
    }
}
