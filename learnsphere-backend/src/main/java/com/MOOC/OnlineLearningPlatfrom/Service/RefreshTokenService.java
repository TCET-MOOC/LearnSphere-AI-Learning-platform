package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Entity.RefreshToken;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import org.springframework.http.ResponseCookie;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(UserAccount user);

    RefreshToken rotateRefreshToken(String rawToken);

    void revokeToken(String rawToken);

    ResponseCookie buildRefreshCookie(String token, boolean secure);

    ResponseCookie buildCleanRefreshCookie(boolean secure);
}
