package com.MOOC.OnlineLearningPlatfrom.Config;

import com.MOOC.OnlineLearningPlatfrom.Entity.AffiliationRequest;
import com.MOOC.OnlineLearningPlatfrom.Entity.College;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;
import com.MOOC.OnlineLearningPlatfrom.Entity.Role;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserRole;
import com.MOOC.OnlineLearningPlatfrom.Repository.AffiliationRequestRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CollegeRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.LectureRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Seeds the documented demo personas and their starter courses.
 *
 * <p>This runner is deliberately absent from {@code local} and {@code prod}:
 * production boots with only the reference roles created by {@link RoleSeeder}.
 * The password is always read from {@code app.seed.password}, which the
 * {@code demo} profile maps to the required {@code SEED_PASSWORD} environment
 * variable. There is no password literal in source.</p>
 */
@Component
@Profile({"demo", "test"})
@Order(2)
public class DemoDataSeeder implements CommandLineRunner {

    public static final LocalDateTime SEEDED_AT = LocalDateTime.of(2025, 1, 1, 9, 0);

    public static final String ADMIN_EMAIL = "admin@learnsphere.com";
    public static final String TEACHER_EMAIL = "teacher@learnsphere.com";
    public static final String STUDENT_EMAIL = "student@learnsphere.com";
    public static final String EXTERNAL_STUDENT_EMAIL = "external@learnsphere.com";
    public static final String PENDING_TEACHER_EMAIL = "pending@learnsphere.com";

    public static final String PRIMARY_COLLEGE = "Thakur College of Engineering and Technology (TCET)";
    public static final String EXTERNAL_COLLEGE = "Sinhgad Institute of Technology";
    public static final String DEPARTMENT_NAME = "Computer Engineering";

    public static final String CS201_TITLE = "CS201: Data Structures & Algorithms Mastery";
    public static final String AI301_TITLE = "AI301: Machine Learning & Deep Neural Networks";

    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final CollegeRepository collegeRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AffiliationRequestRepository affiliationRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedPassword;

    public DemoDataSeeder(RoleRepository roleRepository,
                          UserAccountRepository userAccountRepository,
                          UserRoleRepository userRoleRepository,
                          CollegeRepository collegeRepository,
                          DepartmentRepository departmentRepository,
                          CourseRepository courseRepository,
                          LectureRepository lectureRepository,
                          EnrollmentRepository enrollmentRepository,
                          AffiliationRequestRepository affiliationRequestRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.seed.password}") String seedPassword) {
        this.roleRepository = roleRepository;
        this.userAccountRepository = userAccountRepository;
        this.userRoleRepository = userRoleRepository;
        this.collegeRepository = collegeRepository;
        this.departmentRepository = departmentRepository;
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.affiliationRequestRepository = affiliationRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(String... args) {
        if (seedPassword == null || seedPassword.isBlank()) {
            throw new IllegalStateException(
                    "app.seed.password is empty; set SEED_PASSWORD before enabling the demo profile");
        }

        Role adminRole = requireRole("ADMIN");
        Role teacherRole = requireRole("TEACHER");
        Role studentRole = requireRole("STUDENT");

        College primaryCollege = getOrCreateCollege(PRIMARY_COLLEGE, "Mumbai");
        College externalCollege = getOrCreateCollege(EXTERNAL_COLLEGE, "Pune");
        Department department = getOrCreateDepartment(DEPARTMENT_NAME);

        // Keep this order stable: a clean reset must reproduce the same ids.
        UserAccount teacher = getOrCreateUser(
                TEACHER_EMAIL, "Prof. R. K. Sharma", teacherRole, primaryCollege, department);
        UserAccount student = getOrCreateUser(
                STUDENT_EMAIL, "Aarav Sharma", studentRole, primaryCollege, department);
        getOrCreateUser(
                ADMIN_EMAIL, "System Admin", adminRole, primaryCollege, department);
        UserAccount externalStudent = getOrCreateUser(
                EXTERNAL_STUDENT_EMAIL, "Meera Iyer", studentRole, externalCollege, department);
        UserAccount pendingTeacher = getOrCreateUser(
                PENDING_TEACHER_EMAIL, "Nikhil Verma", teacherRole, null, department);

        Course cs201 = seedCourse(
                CS201_TITLE,
                "Master foundational computing concepts: Arrays, Linked Lists, Trees, Graphs, "
                        + "Dijkstra's algorithm, Dynamic Programming, and complexity proofs.",
                "https://img.youtube.com/vi/3JZ_D3ELwOQ/hqdefault.jpg",
                teacher,
                new LectureSeed[]{
                        new LectureSeed(1, "Introduction to Asymptotic Complexity & Big-O", 900,
                                "https://www.youtube-nocookie.com/embed/3JZ_D3ELwOQ?rel=0&modestbranding=1"),
                        new LectureSeed(2, "Binary Trees, BSTs & Balancing Techniques", 1240,
                                "https://www.youtube-nocookie.com/embed/L_LUpnjgPso?rel=0&modestbranding=1"),
                        new LectureSeed(3, "Graph Traversal Algorithms: BFS, DFS & Topological Sort", 1680,
                                "https://www.youtube-nocookie.com/embed/fJ9rUzIMcZQ?rel=0&modestbranding=1"),
                        new LectureSeed(4, "Shortest Path Algorithms: Dijkstra & Priority Queues", 1850,
                                "https://www.youtube-nocookie.com/embed/kJQP7kiw5Fk?rel=0&modestbranding=1")
                });

        seedCourse(
                AI301_TITLE,
                "Comprehensive guide to predictive modeling, gradient descent, classification, "
                        + "convolutional neural networks, and PyTorch production pipelines.",
                "https://img.youtube.com/vi/Gv9_4yMHFhI/hqdefault.jpg",
                teacher,
                new LectureSeed[]{
                        new LectureSeed(1, "Linear Regression & Gradient Descent Optimization", 1140,
                                "https://www.youtube-nocookie.com/embed/Gv9_4yMHFhI?rel=0&modestbranding=1"),
                        new LectureSeed(2, "Logistic Regression & Cross-Entropy Loss", 1320,
                                "https://www.youtube-nocookie.com/embed/ukzFI9rgwfU?rel=0&modestbranding=1"),
                        new LectureSeed(3, "Deep Neural Networks & Backpropagation Proof", 1950,
                                "https://www.youtube-nocookie.com/embed/aircAruvnKk?rel=0&modestbranding=1")
                });

        seedEnrollment(student, cs201);
        seedPendingAffiliation(pendingTeacher, primaryCollege, department);
    }

    private Role requireRole(String name) {
        Role role = roleRepository.findByName(name);
        if (role == null) {
            throw new IllegalStateException("RoleSeeder must create the " + name + " role first");
        }
        return role;
    }

    private College getOrCreateCollege(String name, String city) {
        return collegeRepository.findByName(name).orElseGet(() -> {
            College college = new College();
            college.setName(name);
            college.setCity(city);
            college.setVerificationStatus(College.VerificationStatus.VERIFIED);
            college.setAppliedAt(SEEDED_AT);
            return collegeRepository.save(college);
        });
    }

    private Department getOrCreateDepartment(String name) {
        Department department = departmentRepository.findByName(name);
        if (department == null) {
            department = new Department();
            department.setName(name);
            department.setCreatedAt(SEEDED_AT);
            department = departmentRepository.save(department);
        }
        return department;
    }

    private UserAccount getOrCreateUser(String email,
                                        String fullName,
                                        Role role,
                                        College college,
                                        Department department) {
        UserAccount user = userAccountRepository.findByEmail(email);
        if (user == null) {
            user = new UserAccount();
            user.setEmail(email);
            user.setCreatedAt(SEEDED_AT);
            user.setLastActiveAt(SEEDED_AT);
            user.setLeaderboardPoints(120);
        }
        user.setFullName(fullName);
        user.setPassword(passwordEncoder.encode(seedPassword));
        user.setDepartment(department);
        user.setCollege(college);
        user = userAccountRepository.save(user);

        if (userRoleRepository.findByUser_UserIdAndRole_RoleId(user.getUserId(), role.getRoleId()).isEmpty()) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        }
        return user;
    }

    private Course seedCourse(String title,
                              String description,
                              String thumbnail,
                              UserAccount teacher,
                              LectureSeed[] lectures) {
        Course course = courseRepository.findFirstByTitle(title).orElseGet(() -> {
            Course newCourse = new Course();
            newCourse.setTitle(title);
            Department dept = departmentRepository.findByName(DEPARTMENT_NAME);
            if (dept != null) {
                newCourse.setDepartment(dept);
            }
            newCourse.setDepartmentLabel(DEPARTMENT_NAME);
            newCourse.setStatus(Course.Status.LIVE);
            newCourse.setTeacher(teacher);
            newCourse.setPrice(BigDecimal.ZERO);
            newCourse.setThumbnail(thumbnail);
            newCourse.setCreatedAt(SEEDED_AT);
            return courseRepository.save(newCourse);
        });

        if (lectureRepository.countByCourse_Id(course.getId()) == 0) {
            for (LectureSeed seed : lectures) {
                Lecture lecture = new Lecture();
                lecture.setCourse(course);
                lecture.setNumber(seed.number);
                lecture.setTitle(seed.title);
                lecture.setDuration(seed.durationSeconds);
                lecture.setVideoUrl(seed.videoUrl);
                lecture.setStatus(Lecture.Status.PUBLISHED);
                lectureRepository.save(lecture);
            }
        }
        return course;
    }

    private void seedEnrollment(UserAccount student, Course course) {
        if (course == null || enrollmentRepository.existsByUserIdAndCourse_Id(student.getUserId(), course.getId())) {
            return;
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUserId(student.getUserId());
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(SEEDED_AT);
        enrollmentRepository.save(enrollment);
    }

    private void seedPendingAffiliation(UserAccount pendingTeacher,
                                        College claimedCollege,
                                        Department claimedDepartment) {
        if (affiliationRequestRepository.count() > 0) {
            return;
        }
        AffiliationRequest request = new AffiliationRequest();
        request.setTeacher(pendingTeacher);
        request.setClaimedCollege(claimedCollege);
        request.setClaimedDepartment(claimedDepartment.getName());
        request.setStatus(AffiliationRequest.Status.PENDING);
        request.setCreatedAt(SEEDED_AT);
        affiliationRequestRepository.save(request);
    }

    private static final class LectureSeed {
        private final int number;
        private final String title;
        private final int durationSeconds;
        private final String videoUrl;

        private LectureSeed(int number, String title, int durationSeconds, String videoUrl) {
            this.number = number;
            this.title = title;
            this.durationSeconds = durationSeconds;
            this.videoUrl = videoUrl;
        }
    }
}
