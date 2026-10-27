package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.RefreshToken;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.RefreshTokenRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofDays(7);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final boolean cookieSecure;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository,
                                  @Value("${app.jwt.cookie-secure:false}") boolean cookieSecure) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.cookieSecure = cookieSecure;
    }

    private String generateSecureTokenString() {
        byte[] randomBytes = new byte[48];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(UserAccount user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(generateSecureTokenString());
        refreshToken.setFamilyId(UUID.randomUUID().toString());
        refreshToken.setStatus(RefreshToken.Status.ACTIVE);
        refreshToken.setExpiresAt(LocalDateTime.now().plus(REFRESH_TOKEN_VALIDITY));
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Refresh token is required");
        }

        RefreshToken currentToken = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        // Replay detection: If token has already been rotated, this is a token reuse attack
        if (currentToken.getStatus() == RefreshToken.Status.ROTATED) {
            refreshTokenRepository.revokeFamily(currentToken.getFamilyId());
            throw new BadCredentialsException("Refresh token reuse detected. Token family revoked.");
        }

        if (currentToken.getStatus() == RefreshToken.Status.REVOKED) {
            throw new BadCredentialsException("Refresh token is revoked");
        }

        if (currentToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token has expired");
        }

        UserAccount user = currentToken.getUser();
        if (user == null || user.getStatus() == UserAccount.Status.BLACKLISTED) {
            throw new BadCredentialsException("User account is deactivated");
        }

        // Rotate token
        String newTokenString = generateSecureTokenString();
        currentToken.setStatus(RefreshToken.Status.ROTATED);
        currentToken.setReplacedByToken(newTokenString);
        refreshTokenRepository.save(currentToken);

        RefreshToken nextToken = new RefreshToken();
        nextToken.setUser(user);
        nextToken.setToken(newTokenString);
        nextToken.setFamilyId(currentToken.getFamilyId());
        nextToken.setStatus(RefreshToken.Status.ACTIVE);
        nextToken.setExpiresAt(LocalDateTime.now().plus(REFRESH_TOKEN_VALIDITY));
        return refreshTokenRepository.save(nextToken);
    }

    @Override
    @Transactional
    public void revokeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByToken(rawToken).ifPresent(token -> {
            token.setStatus(RefreshToken.Status.REVOKED);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    public ResponseCookie buildRefreshCookie(String token, boolean secure) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(secure || this.cookieSecure)
                .path("/api/auth")
                .maxAge(REFRESH_TOKEN_VALIDITY)
                .sameSite("Lax")
                .build();
    }

    @Override
    public ResponseCookie buildCleanRefreshCookie(boolean secure) {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(secure || this.cookieSecure)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }
}
