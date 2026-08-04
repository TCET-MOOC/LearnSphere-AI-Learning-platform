package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.PaymentOrderDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.PaymentResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;

import java.util.List;

public interface PaymentService {
    PaymentOrderDto checkout(Long courseId, CustomUserDetails principal);
    PaymentResponseDto verify(String orderId, CustomUserDetails principal);
    List<PaymentResponseDto> getHistory(CustomUserDetails principal);
    boolean hasPaid(Long courseId, CustomUserDetails principal);
}
