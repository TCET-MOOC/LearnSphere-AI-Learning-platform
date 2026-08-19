package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.math.BigDecimal;

/** One slice of the "royalty breakdown by source" bar chart on the teacher royalties page. */
public class RoyaltySourceSplitDto {
    private String source;
    private String label;
    private BigDecimal amount;
    private double percentage;

    public RoyaltySourceSplitDto(String source, String label, BigDecimal amount, double percentage) {
        this.source = source;
        this.label = label;
        this.amount = amount;
        this.percentage = percentage;
    }

    public String getSource() { return source; }
    public String getLabel() { return label; }
    public BigDecimal getAmount() { return amount; }
    public double getPercentage() { return percentage; }
}
