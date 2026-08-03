package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.AuthDtos.*;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.JwtService;
import com.MOOC.OnlineLearningPlatfrom.Service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final CollegeRepository collegeRepository;
    private final DepartmentRepository departmentRepository;
    private final AffiliationRequestRepository affiliationRequestRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserAccountRepository userAccountRepository,
                            UserRoleRepository userRoleRepository,
                            RoleRepository roleRepository,
                            CollegeRepository collegeRepository,
                            DepartmentRepository departmentRepository,
                            AffiliationRequestRepository affiliationRequestRepository,
                            PasswordResetTokenRepository passwordResetTokenRepository,
                            PasswordEncoder passwordEncoder,
                            JwtService jwtService,
                            AuthenticationManager authenticationManager) {
        this.userAccountRepository = userAccountRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.collegeRepository = collegeRepository;
        this.departmentRepository = departmentRepository;
        this.affiliationRequestRepository = affiliationRequestRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userAccountRepository.findByEmail(request.getEmail()) != null) {
            throw new BadRequestException("An account with this email already exists");
        }
        String roleName = request.getRole() == null ? "STUDENT" : request.getRole().toUpperCase();
        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            throw new BadRequestException("Unknown role: " + roleName);
        }

        UserAccount user = new UserAccount();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (request.getCollegeId() != null) {
            College college = collegeRepository.findById(request.getCollegeId())
                    .orElseThrow(() -> new ResourceNotFoundException("College not found with id: " + request.getCollegeId()));
            user.setCollege(college);
        }
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
            user.setDepartment(department);
        }
        user = userAccountRepository.save(user);

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRoleRepository.save(userRole);

        String token = jwtService.generateToken(user.getEmail(), user.getUserId(), List.of(roleName));
        return new AuthResponse(token, UserResponseDto.from(user, List.of(roleName)));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }

        UserAccount user = userAccountRepository.findByEmail(request.getEmail());
        if (user == null) {
            throw new BadCredentialsException("Invalid email or password");
        }
        user.setLastActiveAt(LocalDateTime.now());
        userAccountRepository.save(user);

        List<String> roles = rolesOf(user);
        String token = jwtService.generateToken(user.getEmail(), user.getUserId(), roles);
        return new AuthResponse(token, UserResponseDto.from(user, roles));
    }

    @Override
    public UserResponseDto currentUser(String email) {
        UserAccount user = userAccountRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return UserResponseDto.from(user, rolesOf(user));
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        UserAccount user = userAccountRepository.findByEmail(request.getEmail());
        if (user == null) {
            // Do not leak whether the email exists.
            return;
        }
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(resetToken);
        // No SMTP configured in this project: log the reset link instead of emailing it.
        log.info("Password reset requested for {}. Reset token: {}", user.getEmail(), resetToken.getToken());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken());
        if (resetToken == null || resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This reset link is invalid or has expired");
        }
        UserAccount user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userAccountRepository.save(user);
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    @Override
    @Transactional
    public UserResponseDto verifyCollege(String email, VerifyCollegeRequest request) {
        UserAccount user = userAccountRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        List<String> roles = rolesOf(user);
        College college = collegeRepository.findById(request.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("College not found with id: " + request.getCollegeId()));

        if (roles.contains("TEACHER")) {
            AffiliationRequest affiliationRequest = new AffiliationRequest();
            affiliationRequest.setTeacher(user);
            affiliationRequest.setClaimedCollege(college);
            affiliationRequest.setClaimedDepartment(request.getClaimedDepartment());
            affiliationRequest.setIdDocumentUrl(request.getIdDocumentUrl());
            affiliationRequestRepository.save(affiliationRequest);
        } else {
            user.setCollege(college);
            userAccountRepository.save(user);
        }
        return UserResponseDto.from(user, roles);
    }

    private List<String> rolesOf(UserAccount user) {
        return userRoleRepository.findByUser_UserId(user.getUserId())
                .stream().map(UserRole::getRole).map(Role::getName).toList();
    }
}
