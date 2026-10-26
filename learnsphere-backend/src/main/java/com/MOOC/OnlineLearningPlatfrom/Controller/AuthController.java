package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.AuthDtos.*;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody(required = false) RefreshTokenRequest request,
                                                @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = request != null && request.getRefreshToken() != null ? request.getRefreshToken() : null;
        if (token == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        if (token == null) {
            throw new com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException("Refresh token is required");
        }
        return ResponseEntity.ok(authService.refreshToken(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> me(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(authService.currentUser(principal.getUsername()));
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String resetToken = authService.forgotPassword(request);
        Map<String, String> body = new java.util.HashMap<>();
        body.put("message", "If an account exists for that email, a reset link has been sent.");
        // No SMTP is configured in this project: the token is returned directly instead of emailed
        // so the reset flow stays usable end-to-end. A real deployment would drop this field.
        if (resetToken != null) {
            body.put("resetToken", resetToken);
        }
        return ResponseEntity.ok(body);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
    }

    @PostMapping("/verify-college")
    public ResponseEntity<UserResponseDto> verifyCollege(@AuthenticationPrincipal CustomUserDetails principal,
                                                           @RequestBody VerifyCollegeRequest request) {
        return ResponseEntity.ok(authService.verifyCollege(principal.getUsername(), request));
    }
}
