package com.MOOC.OnlineLearningPlatfrom.Dto;

public class RecommendationDto {
    private String title;
    private String description;
    private String action;
    private String actionClass;

    public RecommendationDto(String title, String description, String action, String actionClass) {
        this.title = title;
        this.description = description;
        this.action = action;
        this.actionClass = actionClass;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAction() { return action; }
    public String getActionClass() { return actionClass; }
}
