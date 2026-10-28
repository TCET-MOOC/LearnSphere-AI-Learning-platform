package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.CollegeEntitlementResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.CollegeEntitlementStatusDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.College;
import com.MOOC.OnlineLearningPlatfrom.Entity.CollegeEntitlement;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CollegeEntitlementRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.AuditService;
import com.MOOC.OnlineLearningPlatfrom.Service.CollegeEntitlementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CollegeEntitlementServiceImpl implements CollegeEntitlementService {

    private final CollegeEntitlementRepository collegeEntitlementRepository;
    private final CourseRepository courseRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public CollegeEntitlementServiceImpl(CollegeEntitlementRepository collegeEntitlementRepository,
                                         CourseRepository courseRepository,
                                         UserAccountRepository userAccountRepository,
                                         AuditService auditService) {
        this.collegeEntitlementRepository = collegeEntitlementRepository;
        this.courseRepository = courseRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public CollegeEntitlementStatusDto checkEntitlement(Long studentId, Long courseId) {
        if (studentId == null || courseId == null) {
            return CollegeEntitlementStatusDto.builder().eligible(false).reason("NONE").courseId(courseId).build();
        }

        // 1. Check existing unrevoked entitlement in database
        Optional<CollegeEntitlement> existing = collegeEntitlementRepository.findByStudent_UserIdAndCourse_Id(studentId, courseId);
        if (existing.isPresent() && Boolean.FALSE.equals(existing.get().getRevoked())) {
            CollegeEntitlement ent = existing.get();
            return CollegeEntitlementStatusDto.builder()
                    .eligible(true)
                    .reason(ent.getGrantType() != null ? ent.getGrantType().name() : "SAME_COLLEGE")
                    .collegeName(ent.getCollege() != null ? ent.getCollege().getName() : null)
                    .courseId(courseId)
                    .build();
        }

        // If explicitly revoked, do not auto-grant same-college entitlement
        if (existing.isPresent() && Boolean.TRUE.equals(existing.get().getRevoked())) {
            return CollegeEntitlementStatusDto.builder().eligible(false).reason("NONE").courseId(courseId).build();
        }

        // 2. Check same-college eligibility
        UserAccount student = userAccountRepository.findById(studentId).orElse(null);
        Course course = courseRepository.findById(courseId).orElse(null);

        if (student != null && course != null && isSameVerifiedCollege(student, course)) {
            return CollegeEntitlementStatusDto.builder()
                    .eligible(true)
                    .reason("SAME_COLLEGE")
                    .collegeName(student.getCollege().getName())
                    .courseId(courseId)
                    .build();
        }

        return CollegeEntitlementStatusDto.builder().eligible(false).reason("NONE").courseId(courseId).build();
    }

    @Override
    @Transactional
    public CollegeEntitlement grantSameCollegeEntitlement(Long studentId, Long courseId) {
        UserAccount student = userAccountRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        if (!isSameVerifiedCollege(student, course)) {
            throw new BadRequestException("Student and course teacher do not belong to the same verified college");
        }

        Optional<CollegeEntitlement> existing = collegeEntitlementRepository.findByStudent_UserIdAndCourse_Id(studentId, courseId);
        CollegeEntitlement entitlement;
        if (existing.isPresent()) {
            entitlement = existing.get();
            entitlement.setRevoked(false);
            entitlement.setRevokedAt(null);
            entitlement.setRevokedBy(null);
            entitlement.setCollege(student.getCollege());
            entitlement.setGrantType(CollegeEntitlement.GrantType.SAME_COLLEGE);
            entitlement.setGrantedAt(LocalDateTime.now());
        } else {
            entitlement = new CollegeEntitlement();
            entitlement.setStudent(student);
            entitlement.setCourse(course);
            entitlement.setCollege(student.getCollege());
            entitlement.setGrantType(CollegeEntitlement.GrantType.SAME_COLLEGE);
            entitlement.setGrantedAt(LocalDateTime.now());
            entitlement.setRevoked(false);
        }

        entitlement = collegeEntitlementRepository.save(entitlement);

        if (auditService != null) {
            auditService.record(
                    AuditService.ENTITLEMENT_GRANTED,
                    AuditService.RESOURCE_ENTITLEMENT,
                    entitlement.getId(),
                    null,
                    Map.of(
                            "studentId", studentId,
                            "courseId", courseId,
                            "grantType", "SAME_COLLEGE",
                            "collegeId", student.getCollege().getId()
                    )
            );
        }

        return entitlement;
    }

    @Override
    @Transactional
    public CollegeEntitlement grantAdminOverride(Long studentId, Long courseId, Long adminId, String reason) {
        UserAccount student = userAccountRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        UserAccount admin = adminId != null ? userAccountRepository.findById(adminId).orElse(null) : null;

        Optional<CollegeEntitlement> existing = collegeEntitlementRepository.findByStudent_UserIdAndCourse_Id(studentId, courseId);
        CollegeEntitlement entitlement;
        if (existing.isPresent()) {
            entitlement = existing.get();
            entitlement.setRevoked(false);
            entitlement.setRevokedAt(null);
            entitlement.setRevokedBy(null);
            entitlement.setGrantType(CollegeEntitlement.GrantType.ADMIN_OVERRIDE);
            entitlement.setGrantedAt(LocalDateTime.now());
            entitlement.setGrantedBy(admin);
            entitlement.setReason(reason);
            if (student.getCollege() != null) {
                entitlement.setCollege(student.getCollege());
            }
        } else {
            entitlement = new CollegeEntitlement();
            entitlement.setStudent(student);
            entitlement.setCourse(course);
            entitlement.setCollege(student.getCollege());
            entitlement.setGrantType(CollegeEntitlement.GrantType.ADMIN_OVERRIDE);
            entitlement.setGrantedAt(LocalDateTime.now());
            entitlement.setGrantedBy(admin);
            entitlement.setRevoked(false);
            entitlement.setReason(reason);
        }

        entitlement = collegeEntitlementRepository.save(entitlement);

        if (auditService != null) {
            auditService.record(
                    AuditService.ENTITLEMENT_GRANTED,
                    AuditService.RESOURCE_ENTITLEMENT,
                    entitlement.getId(),
                    null,
                    Map.of(
                            "studentId", studentId,
                            "courseId", courseId,
                            "grantType", "ADMIN_OVERRIDE",
                            "adminId", adminId != null ? adminId : 0L,
                            "reason", reason != null ? reason : ""
                    )
            );
        }

        return entitlement;
    }

    @Override
    @Transactional
    public void revokeEntitlement(Long entitlementId, Long adminId, String reason) {
        CollegeEntitlement entitlement = collegeEntitlementRepository.findById(entitlementId)
                .orElseThrow(() -> new ResourceNotFoundException("College entitlement not found with id: " + entitlementId));

        UserAccount admin = adminId != null ? userAccountRepository.findById(adminId).orElse(null) : null;

        entitlement.setRevoked(true);
        entitlement.setRevokedAt(LocalDateTime.now());
        entitlement.setRevokedBy(admin);
        if (reason != null && !reason.isBlank()) {
            entitlement.setReason(reason);
        }
        collegeEntitlementRepository.save(entitlement);

        if (auditService != null) {
            auditService.record(
                    AuditService.ENTITLEMENT_REVOKED,
                    AuditService.RESOURCE_ENTITLEMENT,
                    entitlementId,
                    Map.of("revoked", false),
                    Map.of(
                            "revoked", true,
                            "adminId", adminId != null ? adminId : 0L,
                            "reason", reason != null ? reason : ""
                    )
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollegeEntitlementResponseDto> getStudentEntitlements(Long studentId) {
        return collegeEntitlementRepository.findByStudent_UserId(studentId).stream()
                .map(CollegeEntitlementResponseDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollegeEntitlementResponseDto> getCourseEntitlements(Long courseId) {
        return collegeEntitlementRepository.findByCourse_Id(courseId).stream()
                .map(CollegeEntitlementResponseDto::from)
                .toList();
    }

    private boolean isSameVerifiedCollege(UserAccount student, Course course) {
        College studentCollege = student.getCollege();
        if (studentCollege == null || studentCollege.getVerificationStatus() != College.VerificationStatus.VERIFIED) {
            return false;
        }

        UserAccount teacher = course.getTeacher();
        if (teacher == null) {
            return false;
        }

        College teacherCollege = teacher.getCollege();
        if (teacherCollege == null || teacherCollege.getVerificationStatus() != College.VerificationStatus.VERIFIED) {
            return false;
        }

        return studentCollege.getId().equals(teacherCollege.getId());
    }
}
