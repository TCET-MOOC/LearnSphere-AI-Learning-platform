package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.AuthDtos.ForgotPasswordRequest;
import com.MOOC.OnlineLearningPlatfrom.Entity.PasswordResetToken;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.AffiliationRequestRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CollegeRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.PasswordResetTokenRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserRoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class AuthServiceLoggingTest {

    @Test
    void shouldNotLogThePasswordResetToken(CapturedOutput output) {
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        PasswordResetTokenRepository passwordResetTokenRepository =
                mock(PasswordResetTokenRepository.class);

        UserAccount user = new UserAccount();
        user.setEmail("student@example.com");
        when(userAccountRepository.findByEmail("student@example.com")).thenReturn(user);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthServiceImpl service = new AuthServiceImpl(
                userAccountRepository,
                mock(UserRoleRepository.class),
                mock(RoleRepository.class),
                mock(CollegeRepository.class),
                mock(DepartmentRepository.class),
                mock(AffiliationRequestRepository.class),
                passwordResetTokenRepository,
                mock(PasswordEncoder.class),
                mock(JwtService.class),
                mock(AuthenticationManager.class));

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("student@example.com");

        String token = service.forgotPassword(request);

        assertThat(token).isNotBlank();
        assertThat(output).contains("student@example.com");
        assertThat(output).doesNotContain(token);
    }
}
