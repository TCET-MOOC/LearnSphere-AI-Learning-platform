package com.MOOC.OnlineLearningPlatfrom.Config;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final CollegeRepository collegeRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(RoleRepository roleRepository,
                      UserAccountRepository userAccountRepository,
                      UserRoleRepository userRoleRepository,
                      CollegeRepository collegeRepository,
                      DepartmentRepository departmentRepository,
                      CourseRepository courseRepository,
                      LectureRepository lectureRepository,
                      EnrollmentRepository enrollmentRepository,
                      PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userAccountRepository = userAccountRepository;
        this.userRoleRepository = userRoleRepository;
        this.collegeRepository = collegeRepository;
        this.departmentRepository = departmentRepository;
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Seed Roles
        Role studentRole = getOrCreateRole("STUDENT");
        Role teacherRole = getOrCreateRole("TEACHER");
        Role adminRole = getOrCreateRole("ADMIN");

        // 2. Seed Sample College & Department
        College college = collegeRepository.findAll().stream().findFirst().orElseGet(() -> {
            College c = new College();
            c.setName("Thakur College of Engineering and Technology (TCET)");
            c.setCity("Mumbai");
            c.setVerificationStatus(College.VerificationStatus.VERIFIED);
            return collegeRepository.save(c);
        });

        Department dept = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Computer Engineering");
            return departmentRepository.save(d);
        });

        // 3. Seed Default Accounts
        UserAccount teacher = getOrCreateUser("teacher@learnsphere.com", "Prof. R. K. Sharma", teacherRole, college, dept);
        UserAccount student = getOrCreateUser("student@learnsphere.com", "Aarav Sharma", studentRole, college, dept);
        getOrCreateUser("admin@learnsphere.com", "System Admin", adminRole, college, dept);

        // 4. Seed Default Starter Courses if none exist
        if (courseRepository.count() == 0) {
            Course c1 = new Course();
            c1.setTitle("CS201: Data Structures & Algorithms Mastery");
            c1.setDescription("Master foundational computing concepts: Arrays, Linked Lists, Trees, Graphs, Dijkstra's algorithm, Dynamic Programming, and complexity proofs.");
            c1.setDepartment("Computer Engineering");
            c1.setStatus(Course.Status.LIVE);
            c1.setTeacher(teacher);
            c1.setPrice(BigDecimal.ZERO);
            c1.setThumbnail("https://img.youtube.com/vi/3JZ_D3ELwOQ/hqdefault.jpg");
            Course savedC1 = courseRepository.save(c1);

            seedLecture(savedC1, 1, "Introduction to Asymptotic Complexity & Big-O", 900, "https://www.youtube-nocookie.com/embed/3JZ_D3ELwOQ?rel=0&modestbranding=1");
            seedLecture(savedC1, 2, "Binary Trees, BSTs & Balancing Techniques", 1240, "https://www.youtube-nocookie.com/embed/L_LUpnjgPso?rel=0&modestbranding=1");
            seedLecture(savedC1, 3, "Graph Traversal Algorithms: BFS, DFS & Topological Sort", 1680, "https://www.youtube-nocookie.com/embed/fJ9rUzIMcZQ?rel=0&modestbranding=1");
            seedLecture(savedC1, 4, "Shortest Path Algorithms: Dijkstra & Priority Queues", 1850, "https://www.youtube-nocookie.com/embed/kJQP7kiw5Fk?rel=0&modestbranding=1");

            Course c2 = new Course();
            c2.setTitle("AI301: Machine Learning & Deep Neural Networks");
            c2.setDescription("Comprehensive guide to predictive modeling, gradient descent, classification, convolutional neural networks, and PyTorch production pipelines.");
            c2.setDepartment("Computer Engineering");
            c2.setStatus(Course.Status.LIVE);
            c2.setTeacher(teacher);
            c2.setPrice(BigDecimal.ZERO);
            c2.setThumbnail("https://img.youtube.com/vi/Gv9_4yMHFhI/hqdefault.jpg");
            Course savedC2 = courseRepository.save(c2);

            seedLecture(savedC2, 1, "Linear Regression & Gradient Descent Optimization", 1140, "https://www.youtube-nocookie.com/embed/Gv9_4yMHFhI?rel=0&modestbranding=1");
            seedLecture(savedC2, 2, "Logistic Regression & Cross-Entropy Loss", 1320, "https://www.youtube-nocookie.com/embed/ukzFI9rgwfU?rel=0&modestbranding=1");
            seedLecture(savedC2, 3, "Deep Neural Networks & Backpropagation Proof", 1950, "https://www.youtube-nocookie.com/embed/aircAruvnKk?rel=0&modestbranding=1");

            // Seed Enrollment for Default Student in Course 1
            Enrollment enrollment = new Enrollment();
            enrollment.setCourse(savedC1);
            enrollment.setUserId(student.getUserId());
            enrollment.setRole("STUDENT");
            enrollment.setEnrolledAt(LocalDateTime.now());
            enrollmentRepository.save(enrollment);
        }
    }

    private Role getOrCreateRole(String name) {
        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role();
            role.setName(name);
            role = roleRepository.save(role);
        }
        return role;
    }

    private UserAccount getOrCreateUser(String email, String fullName, Role role, College college, Department dept) {
        UserAccount user = userAccountRepository.findByEmail(email);
        if (user == null) {
            user = new UserAccount();
            user.setEmail(email);
            user.setFullName(fullName);
            user.setPassword(passwordEncoder.encode("password123"));
            user.setDepartment(dept);
            user.setCollege(college);
            user.setLeaderboardPoints(120);
            user = userAccountRepository.save(user);

            UserRole ur = new UserRole();
            ur.setUser(user);
            ur.setRole(role);
            userRoleRepository.save(ur);
        }
        return user;
    }

    private void seedLecture(Course course, int number, String title, int durationSeconds, String videoUrl) {
        Lecture lecture = new Lecture();
        lecture.setCourse(course);
        lecture.setNumber(number);
        lecture.setTitle(title);
        lecture.setDuration(durationSeconds);
        lecture.setVideoUrl(videoUrl);
        lecture.setStatus(Lecture.Status.PUBLISHED);
        lectureRepository.save(lecture);
    }
}
