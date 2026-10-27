package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.Department;
import com.MOOC.OnlineLearningPlatfrom.Entity.Role;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserRole;
import com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserRoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.JwtService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtClaimsAndLifetimeTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("JWT lifetime is configured to 15 minutes (900,000 ms)")
    void shouldConfigure15MinuteLifetime() {
        assertThat(expirationMs).isEqualTo(15 * 60 * 1000L);
    }

    @Test
    @DisplayName("Generated token carries userId, roles, and dept_id claims with 15-minute expiration")
    void shouldIncludeRequiredClaimsAndLifetime() {
        String token = jwtService.generateToken("student.jwt@learnsphere.com", 123L, List.of("STUDENT"), 42L);

        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("student.jwt@learnsphere.com");
        assertThat(claims.get("userId", Long.class)).isEqualTo(123L);
        assertThat(claims.get("roles", List.class)).containsExactly("STUDENT");
        assertThat(claims.get("dept_id", Long.class)).isEqualTo(42L);
        assertThat(claims.getIssuer()).isEqualTo("learnsphere");
        assertThat(claims.getAudience()).contains("learnsphere-web");

        long lifetimeMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(lifetimeMillis).isEqualTo(15 * 60 * 1000L);
    }

    @Test
    @DisplayName("User without department carries null dept_id claim")
    void shouldAllowNullDeptIdForUserWithoutDepartment() {
        String token = jwtService.generateToken("admin.jwt@learnsphere.com", 1L, List.of("ADMIN"), null);

        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.get("dept_id")).isNull();
        assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
        assertThat(claims.get("roles", List.class)).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("Token with invalid or missing issuer is rejected")
    void shouldRejectTokenWithInvalidIssuer() throws Exception {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 900000);
        String badIssuerToken = Jwts.builder()
                .subject("student@learnsphere.com")
                .issuer("untrusted-foreign-issuer")
                .audience().add("learnsphere-web").and()
                .claim("userId", 2L)
                .claim("roles", List.of("STUDENT"))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();

        assertThat(jwtService.isTokenValid(badIssuerToken, "student@learnsphere.com")).isFalse();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + badIssuerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token with invalid or missing audience is rejected")
    void shouldRejectTokenWithInvalidAudience() throws Exception {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 900000);
        String badAudienceToken = Jwts.builder()
                .subject("student@learnsphere.com")
                .issuer("learnsphere")
                .audience().add("rogue-client").and()
                .claim("userId", 2L)
                .claim("roles", List.of("STUDENT"))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();

        assertThat(jwtService.isTokenValid(badAudienceToken, "student@learnsphere.com")).isFalse();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + badAudienceToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Expired token is rejected")
    void shouldRejectExpiredToken() throws Exception {
        Date past = new Date(System.currentTimeMillis() - 60000);
        Date expiredAt = new Date(System.currentTimeMillis() - 1000);
        String expiredToken = Jwts.builder()
                .subject("student@learnsphere.com")
                .issuer("learnsphere")
                .audience().add("learnsphere-web").and()
                .claim("userId", 2L)
                .claim("roles", List.of("STUDENT"))
                .issuedAt(past)
                .expiration(expiredAt)
                .signWith(key())
                .compact();

        assertThat(jwtService.isTokenValid(expiredToken, "student@learnsphere.com")).isFalse();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login produces token with dept_id, userId, roles, issuer and audience")
    void shouldProduceTokenWithDeptIdAtLogin() throws Exception {
        Department dept = new Department();
        dept.setName("Computer Science");
        dept = departmentRepository.save(dept);

        UserAccount user = new UserAccount();
        user.setFullName("Dept Student");
        user.setEmail("dept.student@learnsphere.com");
        user.setPassword(passwordEncoder.encode("Password@123"));
        user.setStatus(UserAccount.Status.ACTIVE);
        user.setDepartment(dept);
        user = userAccountRepository.save(user);

        Role studentRole = roleRepository.findByName("STUDENT");
        if (studentRole == null) {
            Role r = new Role();
            r.setName("STUDENT");
            studentRole = roleRepository.save(r);
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(studentRole);
        userRoleRepository.save(userRole);

        String loginJson = """
                {
                    "email": "dept.student@learnsphere.com",
                    "password": "Password@123"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");

        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("dept.student@learnsphere.com");
        assertThat(claims.get("userId", Long.class)).isEqualTo(user.getUserId());
        assertThat(claims.get("roles", List.class)).contains("STUDENT");
        assertThat(claims.get("dept_id", Long.class)).isEqualTo(dept.getDepartmentId());
        assertThat(claims.getIssuer()).isEqualTo("learnsphere");
        assertThat(claims.getAudience()).contains("learnsphere-web");
    }
}
