package com.MOOC.OnlineLearningPlatfrom.Dto;

/** Sentiment split for a course department/type: % of analyzed items classified positive. */
public class CourseTypeSentimentDto {
    private String type;
    private int positive;
    private String color;

    public CourseTypeSentimentDto(String type, int positive, String color) {
        this.type = type;
        this.positive = positive;
        this.color = color;
    }

    public String getType() { return type; }
    public int getPositive() { return positive; }
    public String getColor() { return color; }
}
