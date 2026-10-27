package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.RefreshToken;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.RefreshTokenRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RefreshTokenLifecycleTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("Login sets 7-day HttpOnly refresh cookie and persists active token")
    void shouldSetHttpOnlyRefreshCookieOnLogin() throws Exception {
        String loginJson = """
                {
                    "email": "student@learnsphere.com",
                    "password": "TestPass1!"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth")))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn();

        String refreshTokenValue = result.getResponse().getCookie("refreshToken").getValue();
        assertThat(refreshTokenValue).isNotBlank();

        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenValue).orElseThrow();
        assertThat(token.getUser().getEmail()).isEqualTo("student@learnsphere.com");
        assertThat(token.getStatus()).isEqualTo(RefreshToken.Status.ACTIVE);
        assertThat(token.getFamilyId()).isNotBlank();
        assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(6));
    }

    @Test
    @DisplayName("Refresh rotates token and updates chain")
    void shouldRotateRefreshTokenOnRefresh() throws Exception {
        UserAccount student = userAccountRepository.findByEmail("student@learnsphere.com");

        RefreshToken original = new RefreshToken();
        original.setUser(student);
        original.setToken("original-refresh-token-12345");
        original.setFamilyId("family-uuid-001");
        original.setStatus(RefreshToken.Status.ACTIVE);
        original.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(original);

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "original-refresh-token-12345")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn();

        String newRefreshTokenValue = result.getResponse().getCookie("refreshToken").getValue();
        assertThat(newRefreshTokenValue).isNotEqualTo("original-refresh-token-12345");

        RefreshToken oldToken = refreshTokenRepository.findByToken("original-refresh-token-12345").orElseThrow();
        assertThat(oldToken.getStatus()).isEqualTo(RefreshToken.Status.ROTATED);
        assertThat(oldToken.getReplacedByToken()).isEqualTo(newRefreshTokenValue);

        RefreshToken newToken = refreshTokenRepository.findByToken(newRefreshTokenValue).orElseThrow();
        assertThat(newToken.getStatus()).isEqualTo(RefreshToken.Status.ACTIVE);
        assertThat(newToken.getFamilyId()).isEqualTo("family-uuid-001");
    }

    @Test
    @DisplayName("Replaying a rotated refresh token revokes the entire token family")
    void shouldRevokeFamilyOnRefreshReuseAttack() throws Exception {
        UserAccount student = userAccountRepository.findByEmail("student@learnsphere.com");

        RefreshToken rotatedToken = new RefreshToken();
        rotatedToken.setUser(student);
        rotatedToken.setToken("already-rotated-token");
        rotatedToken.setFamilyId("compromised-family-999");
        rotatedToken.setStatus(RefreshToken.Status.ROTATED);
        rotatedToken.setReplacedByToken("legitimate-successor-token");
        rotatedToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(rotatedToken);

        RefreshToken successor = new RefreshToken();
        successor.setUser(student);
        successor.setToken("legitimate-successor-token");
        successor.setFamilyId("compromised-family-999");
        successor.setStatus(RefreshToken.Status.ACTIVE);
        successor.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(successor);

        // Attacker replays already-rotated token
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "already-rotated-token")))
                .andExpect(status().isUnauthorized());

        // The entire family must now be revoked
        List<RefreshToken> family = refreshTokenRepository.findByFamilyId("compromised-family-999");
        assertThat(family).isNotEmpty();
        for (RefreshToken t : family) {
            assertThat(t.getStatus()).isEqualTo(RefreshToken.Status.REVOKED);
        }

        // Legitimate successor token must also now be rejected
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "legitimate-successor-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Expired refresh token is rejected")
    void shouldRejectExpiredRefreshToken() throws Exception {
        UserAccount student = userAccountRepository.findByEmail("student@learnsphere.com");

        RefreshToken expired = new RefreshToken();
        expired.setUser(student);
        expired.setToken("expired-refresh-token");
        expired.setFamilyId("expired-family");
        expired.setStatus(RefreshToken.Status.ACTIVE);
        expired.setExpiresAt(LocalDateTime.now().minusSeconds(10));
        refreshTokenRepository.save(expired);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "expired-refresh-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logout revokes the refresh token and clears the cookie")
    void shouldRevokeTokenAndClearCookieOnLogout() throws Exception {
        UserAccount student = userAccountRepository.findByEmail("student@learnsphere.com");

        RefreshToken token = new RefreshToken();
        token.setUser(student);
        token.setToken("logout-refresh-token");
        token.setFamilyId("logout-family");
        token.setStatus(RefreshToken.Status.ACTIVE);
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(token);

        MvcResult result = mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refreshToken", "logout-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andReturn();

        RefreshToken revoked = refreshTokenRepository.findByToken("logout-refresh-token").orElseThrow();
        assertThat(revoked.getStatus()).isEqualTo(RefreshToken.Status.REVOKED);

        // Attempting to refresh with the logged-out token must fail
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "logout-refresh-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Blacklisted account cannot refresh token")
    void shouldRejectRefreshForBlacklistedAccount() throws Exception {
        UserAccount user = new UserAccount();
        user.setFullName("Blacklisted Refresher");
        user.setEmail("blacklisted.refresher@learnsphere.com");
        user.setPassword("hashed");
        user.setStatus(UserAccount.Status.BLACKLISTED);
        user = userAccountRepository.save(user);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken("blacklisted-user-refresh-token");
        token.setFamilyId("blacklisted-family");
        token.setStatus(RefreshToken.Status.ACTIVE);
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(token);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "blacklisted-user-refresh-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Missing refresh token returns Bad Request")
    void shouldRejectMissingRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isBadRequest());
    }
}
