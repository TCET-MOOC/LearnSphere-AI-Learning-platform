package com.MOOC.OnlineLearningPlatfrom.Dto;

public class NegativeKeywordDto {
    private String word;
    private int count;
    private String severity;

    public NegativeKeywordDto(String word, int count, String severity) {
        this.word = word;
        this.count = count;
        this.severity = severity;
    }

    public String getWord() { return word; }
    public int getCount() { return count; }
    public String getSeverity() { return severity; }
}
