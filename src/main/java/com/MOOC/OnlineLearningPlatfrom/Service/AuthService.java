package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.AuthDtos.*;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserResponseDto;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponseDto currentUser(String email);
    /**
     * Returns the raw reset token, or null if no account matches the email.
     * No SMTP is configured in this project, so callers surface the token
     * directly instead of emailing it (see AuthController).
     */
    String forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    UserResponseDto verifyCollege(String email, VerifyCollegeRequest request);
}
