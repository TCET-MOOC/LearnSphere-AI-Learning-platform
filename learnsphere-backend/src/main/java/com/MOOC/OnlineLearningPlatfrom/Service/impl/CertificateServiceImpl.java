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
    private final EnrollmentRepository enrollmentRepository;
    private final QuestionRepository questionRepository;
    private final com.MOOC.OnlineLearningPlatfrom.Service.EnrollmentService enrollmentService;

    public CertificateServiceImpl(CertificateRepository certificateRepository,
                                   CourseRepository courseRepository,
                                   LectureRepository lectureRepository,
                                   UserProgressRepository userProgressRepository,
                                   TestRepository testRepository,
                                   TestAttemptRepository testAttemptRepository,
                                   EnrollmentRepository enrollmentRepository,
                                   QuestionRepository questionRepository,
                                   com.MOOC.OnlineLearningPlatfrom.Service.EnrollmentService enrollmentService) {
        this.certificateRepository = certificateRepository;
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.userProgressRepository = userProgressRepository;
        this.testRepository = testRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.questionRepository = questionRepository;
        this.enrollmentService = enrollmentService;
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

        if (!enrollmentRepository.existsByUserIdAndCourse_Id(user.getUserId(), courseId)) {
            throw new BadRequestException("You are not enrolled in this course.");
        }

        Optional<Certificate> existing = certificateRepository
                .findByUser_UserIdAndCourse_IdAndType(user.getUserId(), courseId, type);
        if (existing.isPresent()) {
            return CertificateResponseDto.from(existing.get());
        }

        Double realScore;
        String realGrade;

        if (type == Certificate.Type.STANDARD) {
            var calc = enrollmentService.calculateCourseProgress(courseId, user.getUserId());
            if (calc.getTotalContents() == 0) {
                throw new BadRequestException("This course has no contents or lectures yet, so it cannot be certified.");
            }
            if (!calc.isCompleted()) {
                throw new BadRequestException("You have not completed all contents in this course yet (≥ 90% required). Current progress: " + calc.getProgressPercent() + "%");
            }

            // Check if course has tests and if user took any
            List<Test> courseTests = testRepository.findByCourse_Id(courseId).stream()
                    .filter(t -> t.getIsRemedial() == null || !t.getIsRemedial())
                    .toList();

            int totalMaxMarks = 0;
            int totalScored = 0;
            boolean hasAttempt = false;

            for (Test t : courseTests) {
                int testMax = 0;
                List<Question> questions = questionRepository.findByTest_TestId(t.getTestId());
                for (Question q : questions) {
                    testMax += q.getMarks() != null ? q.getMarks() : 0;
                }
                if (testMax <= 0) continue;
                totalMaxMarks += testMax;

                List<TestAttempt> attempts = testAttemptRepository.findByUserIdAndTest_TestId(user.getUserId(), t.getTestId());
                int best = 0;
                for (TestAttempt a : attempts) {
                    if ("COMPLETED".equalsIgnoreCase(a.getStatus()) && a.getScore() != null) {
                        hasAttempt = true;
                        if (a.getScore() > best) {
                            best = a.getScore();
                        }
                    }
                }
                totalScored += best;
            }

            if (totalMaxMarks > 0 && hasAttempt) {
                double percent = (totalScored * 100.0) / totalMaxMarks;
                realScore = Math.round(percent * 10.0) / 10.0;
            } else {
                realScore = Math.round(calc.getProgressPercent() * 10.0) / 10.0;
            }

            if (realScore >= 90.0) {
                realGrade = "Distinction (" + Math.round(realScore) + "%)";
            } else if (realScore >= 75.0) {
                realGrade = "Merit (" + Math.round(realScore) + "%)";
            } else if (realScore >= 60.0) {
                realGrade = "Credit (" + Math.round(realScore) + "%)";
            } else {
                realGrade = "Pass (" + Math.round(realScore) + "%)";
            }
        } else {
            List<Test> remedialTests = testRepository.findByCourse_IdAndIsRemedial(course.getId(), true);
            if (remedialTests.isEmpty()) {
                throw new BadRequestException("This course has no remedial test configured.");
            }
            Double bestRemedialPercent = null;
            for (Test test : remedialTests) {
                int maxMarks = 0;
                List<Question> questions = questionRepository.findByTest_TestId(test.getTestId());
                for (Question q : questions) {
                    maxMarks += q.getMarks() != null ? q.getMarks() : 0;
                }
                if (maxMarks <= 0) {
                    continue;
                }
                List<TestAttempt> attempts = testAttemptRepository.findByUserIdAndTest_TestId(user.getUserId(), test.getTestId());
                for (TestAttempt attempt : attempts) {
                    if ("COMPLETED".equalsIgnoreCase(attempt.getStatus()) && attempt.getScore() != null) {
                        double percent = (attempt.getScore() * 100.0) / maxMarks;
                        if (percent >= 40.0) {
                            if (bestRemedialPercent == null || percent > bestRemedialPercent) {
                                bestRemedialPercent = percent;
                            }
                        }
                    }
                }
            }
            if (bestRemedialPercent == null) {
                throw new BadRequestException("You have not yet passed a remedial test for this course (40% required).");
            }
            realScore = Math.round(bestRemedialPercent * 10.0) / 10.0;
            realGrade = "Remedial Pass (" + Math.round(realScore) + "%)";
        }

        Certificate certificate = new Certificate();
        certificate.setUser(user);
        certificate.setCourse(course);
        certificate.setType(type);
        certificate.setStudentName(user.getFullName());
        certificate.setInstructorName(course.getTeacher() != null ? course.getTeacher().getFullName() : "Faculty Board");
        certificate.setGrade(realGrade);
        certificate.setScore(realScore);
        certificate.setVerificationCode("LS-" + java.time.Year.now().getValue() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        certificate.setTitle((type == Certificate.Type.REMEDIAL ? "Remedial Certificate - " : "Certificate of Completion - ") + course.getTitle());

        try {
            Certificate saved = certificateRepository.save(certificate);
            return CertificateResponseDto.from(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Concurrent race: record was already created
            return certificateRepository.findByUser_UserIdAndCourse_IdAndType(user.getUserId(), courseId, type)
                    .map(CertificateResponseDto::from)
                    .orElseThrow(() -> ex);
        }
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
}
