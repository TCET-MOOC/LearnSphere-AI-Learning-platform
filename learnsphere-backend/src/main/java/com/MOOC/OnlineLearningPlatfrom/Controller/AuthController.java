package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.AuthDtos.*;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Service.AuthService;
import com.MOOC.OnlineLearningPlatfrom.Service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request);
        ResponseCookie cookie = refreshTokenService.buildRefreshCookie(response.getRefreshToken(),
                httpRequest != null && httpRequest.isSecure());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request);
        ResponseCookie cookie = refreshTokenService.buildRefreshCookie(response.getRefreshToken(),
                httpRequest != null && httpRequest.isSecure());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
                                                @RequestBody(required = false) RefreshTokenRequest request,
                                                @RequestHeader(value = "Authorization", required = false) String authHeader,
                                                HttpServletRequest httpRequest) {
        String token = cookieRefreshToken;
        if ((token == null || token.isBlank()) && request != null) {
            token = request.getRefreshToken();
        }
        if ((token == null || token.isBlank()) && authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        if (token == null || token.isBlank()) {
            throw new com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException("Refresh token is required");
        }
        AuthResponse response = authService.refreshToken(token);
        ResponseCookie cookie = refreshTokenService.buildRefreshCookie(response.getRefreshToken(),
                httpRequest != null && httpRequest.isSecure());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
                                                      @RequestBody(required = false) RefreshTokenRequest request,
                                                      HttpServletRequest httpRequest) {
        String token = cookieRefreshToken;
        if ((token == null || token.isBlank()) && request != null) {
            token = request.getRefreshToken();
        }
        if (token != null && !token.isBlank()) {
            refreshTokenService.revokeToken(token);
        }
        ResponseCookie cookie = refreshTokenService.buildCleanRefreshCookie(httpRequest != null && httpRequest.isSecure());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("message", "Logged out successfully"));
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
