package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.LeaderboardEntryDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.LeaderboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private final UserAccountRepository userAccountRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final CourseRepository courseRepository;
    private final TestRepository testRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final QuestionRepository questionRepository;

    public LeaderboardServiceImpl(UserAccountRepository userAccountRepository,
                                  LeaderboardRepository leaderboardRepository,
                                  CourseRepository courseRepository,
                                  TestRepository testRepository,
                                  TestAttemptRepository testAttemptRepository,
                                  QuestionRepository questionRepository) {
        this.userAccountRepository = userAccountRepository;
        this.leaderboardRepository = leaderboardRepository;
        this.courseRepository = courseRepository;
        this.testRepository = testRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.questionRepository = questionRepository;
    }

    @Override
    public List<LeaderboardEntryDto> getLeaderboard(String scope, CustomUserDetails principal) {
        String normalizedScope = scope == null ? "global" : scope.toLowerCase();

        List<UserAccount> students;
        if (normalizedScope.equals("college")) {
            UserAccount currentUser = principal.getUser();
            if (currentUser.getCollege() == null) {
                return List.of();
            }
            students = userAccountRepository.findCollegeStudentLeaderboard(currentUser.getCollege().getId());
        } else if (normalizedScope.equals("global")) {
            students = userAccountRepository.findGlobalStudentLeaderboard();
        } else {
            throw new BadRequestException("scope must be 'global' or 'college'");
        }

        List<LeaderboardEntryDto> result = new ArrayList<>();
        int rank = 1;
        for (UserAccount student : students) {
            int points = student.getLeaderboardPoints() != null ? student.getLeaderboardPoints() : 0;
            result.add(new LeaderboardEntryDto(rank++, student.getUserId(), student.getFullName(),
                    student.getAvatarUrl(), points));
        }
        return result;
    }

    @Override
    public List<Leaderboard> getCourseLeaderboard(Long courseId) {
        return leaderboardRepository.findByCourse_IdOrderByGlobalRankAsc(courseId);
    }

    @Override
    public List<Leaderboard> getMyLeaderboards(Long userId) {
        return leaderboardRepository.findByUser_UserId(userId);
    }

    @Override
    @Transactional
    public void recalculateCourseLeaderboard(Long courseId) {
        if (courseId == null) return;
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return;

        // Find non-remedial tests for this course
        List<Test> tests = testRepository.findByCourse_Id(courseId).stream()
                .filter(t -> t.getIsRemedial() == null || !t.getIsRemedial())
                .toList();
        if (tests.isEmpty()) return;

        // Collect all distinct student userIds who have completed attempts on tests in this course
        Map<Long, Map<Long, Integer>> userTestBestScores = new HashMap<>(); // userId -> (testId -> bestScore)

        for (Test t : tests) {
            List<TestAttempt> attempts = testAttemptRepository.findByTest_TestId(t.getTestId());
            for (TestAttempt a : attempts) {
                if ("COMPLETED".equalsIgnoreCase(a.getStatus()) && a.getScore() != null && a.getUserId() != null) {
                    userTestBestScores
                            .computeIfAbsent(a.getUserId(), k -> new HashMap<>())
                            .merge(t.getTestId(), a.getScore(), Math::max);
                }
            }
        }

        if (userTestBestScores.isEmpty()) return;

        // Precompute max marks for each test
        Map<Long, Integer> testMaxMarks = new HashMap<>();
        for (Test t : tests) {
            List<Question> questions = questionRepository.findByTest_TestId(t.getTestId());
            int maxMarks = 0;
            for (Question q : questions) {
                maxMarks += q.getMarks() != null ? q.getMarks() : 0;
            }
            testMaxMarks.put(t.getTestId(), maxMarks);
        }

        // For each user, calculate average percentage score
        List<Leaderboard> entries = new ArrayList<>();

        for (Map.Entry<Long, Map<Long, Integer>> userEntry : userTestBestScores.entrySet()) {
            Long userId = userEntry.getKey();
            Map<Long, Integer> scores = userEntry.getValue();

            UserAccount user = userAccountRepository.findById(userId).orElse(null);
            if (user == null) continue;

            double sumPercents = 0.0;
            int count = 0;

            for (Map.Entry<Long, Integer> scoreEntry : scores.entrySet()) {
                Long testId = scoreEntry.getKey();
                int score = scoreEntry.getValue();
                int max = testMaxMarks.getOrDefault(testId, 0);
                double percent;
                if (max > 0) {
                    percent = (score * 100.0) / max;
                } else {
                    percent = (double) score;
                }
                sumPercents += percent;
                count++;
            }

            if (count == 0) continue;

            double avgPercent = sumPercents / count;
            BigDecimal avgScore = BigDecimal.valueOf(avgPercent).setScale(2, RoundingMode.HALF_UP);

            Leaderboard lb = leaderboardRepository.findByUser_UserIdAndCourse_Id(userId, courseId)
                    .orElseGet(() -> {
                        Leaderboard newLb = new Leaderboard();
                        newLb.setUser(user);
                        newLb.setCourse(course);
                        return newLb;
                    });

            lb.setAvgScore(avgScore);
            lb.setUpdatedAt(LocalDateTime.now());
            entries.add(lb);
        }

        // 1. Assign global_rank: sorted by avgScore DESC, updatedAt ASC
        entries.sort(Comparator.comparing(Leaderboard::getAvgScore, Comparator.reverseOrder())
                .thenComparing(Leaderboard::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())));

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setGlobalRank(i + 1);
        }

        // 2. Assign dept_rank: grouped by department
        Map<Long, List<Leaderboard>> deptGroups = new HashMap<>();
        for (Leaderboard lb : entries) {
            Long deptId = (lb.getUser() != null && lb.getUser().getDepartment() != null && lb.getUser().getDepartment().getDepartmentId() != null)
                    ? lb.getUser().getDepartment().getDepartmentId().longValue()
                    : -1L;
            deptGroups.computeIfAbsent(deptId, k -> new ArrayList<>()).add(lb);
        }

        for (List<Leaderboard> group : deptGroups.values()) {
            group.sort(Comparator.comparing(Leaderboard::getAvgScore, Comparator.reverseOrder())
                    .thenComparing(Leaderboard::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
            for (int i = 0; i < group.size(); i++) {
                group.get(i).setDeptRank(i + 1);
            }
        }

        leaderboardRepository.saveAll(entries);
    }
}
