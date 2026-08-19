package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.CertificateResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Service.CertificateService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final UserProgressRepository userProgressRepository;
    private final TestRepository testRepository;
    private final TestAttemptRepository testAttemptRepository;

    public CertificateServiceImpl(CertificateRepository certificateRepository,
                                   CourseRepository courseRepository,
                                   LectureRepository lectureRepository,
                                   UserProgressRepository userProgressRepository,
                                   TestRepository testRepository,
                                   TestAttemptRepository testAttemptRepository) {
        this.certificateRepository = certificateRepository;
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.userProgressRepository = userProgressRepository;
        this.testRepository = testRepository;
        this.testAttemptRepository = testAttemptRepository;
    }

    @Override
    public List<CertificateResponseDto> getCertificatesForUser(Long userId) {
        return certificateRepository.findByUser_UserId(userId).stream()
                .map(CertificateResponseDto::from)
                .toList();
    }

    @Override
    public CertificateResponseDto issueCertificate(UserAccount user, Long courseId, Certificate.Type type) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        Optional<Certificate> existing = certificateRepository
                .findByUser_UserIdAndCourse_IdAndType(user.getUserId(), courseId, type);
        if (existing.isPresent()) {
            return CertificateResponseDto.from(existing.get());
        }

        if (type == Certificate.Type.STANDARD) {
            assertStandardEligibility(user.getUserId(), course);
        } else {
            assertRemedialEligibility(user.getUserId(), course);
        }

        Certificate certificate = new Certificate();
        certificate.setUser(user);
        certificate.setCourse(course);
        certificate.setType(type);
        certificate.setStudentName(user.getFullName());
        certificate.setInstructorName(course.getTeacher() != null ? course.getTeacher().getFullName() : "Faculty Board");
        certificate.setGrade(type == Certificate.Type.REMEDIAL ? "Passing Grade" : "Distinction");
        certificate.setVerificationCode("LS-" + java.time.Year.now().getValue() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        certificate.setTitle((type == Certificate.Type.REMEDIAL ? "Remedial Certificate - " : "Certificate of Completion - ") + course.getTitle());
        Certificate saved = certificateRepository.save(certificate);
        return CertificateResponseDto.from(saved);
    }

    @Override
    public com.MOOC.OnlineLearningPlatfrom.Dto.CertificateVerificationDto verifyCertificate(String verificationCode) {
        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            return com.MOOC.OnlineLearningPlatfrom.Dto.CertificateVerificationDto.invalid(verificationCode);
        }
        return certificateRepository.findByVerificationCode(verificationCode.trim().toUpperCase())
                .map(com.MOOC.OnlineLearningPlatfrom.Dto.CertificateVerificationDto::valid)
                .orElseGet(() -> com.MOOC.OnlineLearningPlatfrom.Dto.CertificateVerificationDto.invalid(verificationCode));
    }

    private void assertStandardEligibility(Long userId, Course course) {
        List<Lecture> lectures = lectureRepository.findByCourse_IdOrderByNumberAsc(course.getId());
        if (lectures.isEmpty()) {
            throw new BadRequestException("This course has no lectures yet, so it cannot be certified.");
        }
        for (Lecture lecture : lectures) {
            Optional<UserProgress> progress = userProgressRepository.findByUserIdAndLecture_Id(userId, lecture.getId());
            if (progress.isEmpty() || progress.get().getProgressPercent() == null || progress.get().getProgressPercent() < 100) {
                throw new BadRequestException("You have not completed all lectures in this course yet.");
            }
        }
    }

    private void assertRemedialEligibility(Long userId, Course course) {
        List<Test> remedialTests = testRepository.findByCourse_IdAndIsRemedial(course.getId(), true);
        if (remedialTests.isEmpty()) {
            throw new BadRequestException("This course has no remedial test configured.");
        }
        for (Test test : remedialTests) {
            int maxMarks = 0;
            if (test.getQuestions() != null) {
                for (Question q : test.getQuestions()) {
                    maxMarks += q.getMarks() != null ? q.getMarks() : 0;
                }
            }
            if (maxMarks <= 0) {
                continue;
            }
            List<TestAttempt> attempts = testAttemptRepository.findByUserIdAndTest_TestId(userId, test.getTestId());
            for (TestAttempt attempt : attempts) {
                if ("COMPLETED".equalsIgnoreCase(attempt.getStatus()) && attempt.getScore() != null) {
                    double percent = (attempt.getScore() * 100.0) / maxMarks;
                    if (percent >= 40.0) {
                        return;
                    }
                }
            }
        }
        throw new BadRequestException("You have not yet passed a remedial test for this course (40% required).");
    }
}
