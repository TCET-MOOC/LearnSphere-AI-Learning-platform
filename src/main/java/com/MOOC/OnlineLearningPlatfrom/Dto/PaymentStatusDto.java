package com.MOOC.OnlineLearningPlatfrom.Dto;

/** Response for GET /api/payments/status?courseId= — used by the frontend payment guard. */
public class PaymentStatusDto {
    private boolean paid;

    public PaymentStatusDto(boolean paid) {
        this.paid = paid;
    }

    public boolean isPaid() { return paid; }
}
