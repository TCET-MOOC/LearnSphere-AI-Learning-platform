package com.MOOC.OnlineLearningPlatfrom.Dto;

public class UnreadCountResponseDto {
    private long count;

    public UnreadCountResponseDto(long count) {
        this.count = count;
    }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
