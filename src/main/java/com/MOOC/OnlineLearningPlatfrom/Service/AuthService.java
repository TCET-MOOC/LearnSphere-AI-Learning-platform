package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.AuthDtos.*;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserResponseDto;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponseDto currentUser(String email);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    UserResponseDto verifyCollege(String email, VerifyCollegeRequest request);
}
