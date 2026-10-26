package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Config.DemoDataSeeder;
import com.MOOC.OnlineLearningPlatfrom.Entity.AffiliationRequest;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.AffiliationRequestRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CollegeRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserRoleRepository;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OnlineLearningPlatfromApplicationTests extends PostgresIntegrationTest {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Environment environment;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private EnrollmentRepository enrollmentRepository;

	@Autowired
	private CollegeRepository collegeRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private AffiliationRequestRepository affiliationRequestRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${app.seed.password}")
	private String seedPassword;

	@Test
	void contextLoads() {
	}

	@Test
	void shouldRunUnderTheTestProfileOnly() {
		assertThat(environment.getActiveProfiles())
				.contains("test")
				.doesNotContain("local");
	}

	@Test
	void shouldExposeReadinessProbeWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/actuator/health/readiness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void shouldBuildTheSchemaFromFlywayAndValidateItWithHibernate() {
		assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
				.as("Hibernate must not own schema changes")
				.isEqualTo("validate");

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from flyway_schema_history where version = '1' and success", Integer.class))
				.as("Flyway must record the baseline as applied")
				.isEqualTo(1);

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from information_schema.tables "
						+ "where table_schema = 'public' and table_name = 'users'",
				Integer.class))
				.as("The baseline must create the application schema")
				.isEqualTo(1);
	}

	@Test
	void shouldUseDisposablePostgresInsteadOfTheLocalDatabase() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			assertThat(connection.getMetaData().getURL())
					.as("Tests must connect to the Testcontainers PostgreSQL instance")
					.isEqualTo(POSTGRES.getJdbcUrl());
		}
		assertThat(POSTGRES.isRunning()).isTrue();
	}

	@Test
	void shouldSeedTheRoleVocabularyInTheTestDatabase() {
		assertThat(roleRepository.count()).isEqualTo(3);
		assertThat(roleRepository.findByName("STUDENT")).isNotNull();
		assertThat(roleRepository.findByName("TEACHER")).isNotNull();
		assertThat(roleRepository.findByName("ADMIN")).isNotNull();
	}

	@Test
	void shouldSeedTheDocumentedDemoPersonasUnderTheTestProfile() {
		assertThat(applicationContext.getBeansOfType(DemoDataSeeder.class))
				.as("the demo seeder must be active under test")
				.hasSize(1);

		UserAccount admin = userAccountRepository.findByEmail(DemoDataSeeder.ADMIN_EMAIL);
		UserAccount teacher = userAccountRepository.findByEmail(DemoDataSeeder.TEACHER_EMAIL);
		UserAccount student = userAccountRepository.findByEmail(DemoDataSeeder.STUDENT_EMAIL);
		UserAccount external = userAccountRepository.findByEmail(DemoDataSeeder.EXTERNAL_STUDENT_EMAIL);
		UserAccount pending = userAccountRepository.findByEmail(DemoDataSeeder.PENDING_TEACHER_EMAIL);

		assertThat(admin).isNotNull();
		assertThat(teacher).isNotNull();
		assertThat(student).isNotNull();
		assertThat(external).isNotNull();
		assertThat(pending).isNotNull();

		assertThat(userRoleRepository.findByUser_UserId(admin.getUserId()))
				.extracting(userRole -> userRole.getRole().getName())
				.containsExactly("ADMIN");
		assertThat(userRoleRepository.findByUser_UserId(teacher.getUserId()))
				.extracting(userRole -> userRole.getRole().getName())
				.containsExactly("TEACHER");
		assertThat(userRoleRepository.findByUser_UserId(student.getUserId()))
				.extracting(userRole -> userRole.getRole().getName())
				.containsExactly("STUDENT");
		assertThat(userRoleRepository.findByUser_UserId(pending.getUserId()))
				.extracting(userRole -> userRole.getRole().getName())
				.containsExactly("TEACHER");

		assertThat(external.getCollege().getName()).isEqualTo(DemoDataSeeder.EXTERNAL_COLLEGE);
		assertThat(pending.getCollege()).isNull();
		assertThat(collegeRepository.count()).isEqualTo(2);
		assertThat(departmentRepository.count()).isEqualTo(1);

		assertThat(courseRepository.countByTeacher_UserId(teacher.getUserId())).isEqualTo(2);
		assertThat(courseRepository.findFirstByTitle(DemoDataSeeder.CS201_TITLE)).isPresent();
		assertThat(courseRepository.findFirstByTitle(DemoDataSeeder.AI301_TITLE)).isPresent();
		assertThat(enrollmentRepository.existsByUserIdAndCourse_Id(
				student.getUserId(),
				courseRepository.findFirstByTitle(DemoDataSeeder.CS201_TITLE).orElseThrow().getId()))
				.isTrue();

		assertThat(affiliationRequestRepository.count()).isEqualTo(1);
		AffiliationRequest affiliation = affiliationRequestRepository.findAll().get(0);
		assertThat(affiliation.getTeacher().getEmail()).isEqualTo(DemoDataSeeder.PENDING_TEACHER_EMAIL);
		assertThat(affiliation.getStatus()).isEqualTo(AffiliationRequest.Status.PENDING);

		assertThat(passwordEncoder.matches(seedPassword, student.getPassword()))
				.as("demo users must authenticate with the configured seed password")
				.isTrue();
		assertThat(student.getPassword()).isNotEqualTo(seedPassword);
		assertThat(student.getPassword()).startsWith("$2");
		assertThat(student.getCreatedAt()).isEqualTo(DemoDataSeeder.SEEDED_AT);
		assertThat(student.getLastActiveAt()).isEqualTo(DemoDataSeeder.SEEDED_AT);
	}

}
