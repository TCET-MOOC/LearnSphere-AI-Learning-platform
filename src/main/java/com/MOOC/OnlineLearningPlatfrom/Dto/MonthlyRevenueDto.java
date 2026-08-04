package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.math.BigDecimal;

public class MonthlyRevenueDto {
    private String month;
    private BigDecimal amount;

    public MonthlyRevenueDto(String month, BigDecimal amount) {
        this.month = month;
        this.amount = amount;
    }

    public String getMonth() { return month; }
    public BigDecimal getAmount() { return amount; }
}
