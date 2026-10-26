package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Config.DemoDataSeeder;
import com.MOOC.OnlineLearningPlatfrom.Repository.AffiliationRequestRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CollegeRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that the production profile boots cleanly but never writes demo data.
 *
 * <p>This class owns its container instead of reusing the shared test-profile
 * database, because the test-profile context deliberately seeds demo rows and
 * that would make an "empty production" assertion meaningless.</p>
 */
@SpringBootTest(properties = {
        "app.jwt.secret=test-only-jwt-secret-for-the-prod-profile-0123456789-abcdefghijklmnopqrstuvwxyz",
        "app.jwt.expiration-ms=900000",
        "internal.api.key=test-only-internal-key",
        "app.cors.allowed-origins=http://localhost:4200",
        "app.upload.dir=target/test-uploads",
        "app.ai.nvidia.api-key=test-key-not-used",
        "app.ai.nvidia.base-url=https://integrate.api.nvidia.com/v1",
        "app.ai.nvidia.model=meta/llama-3.1-70b-instruct",
        "logging.level.org.hibernate.SQL=warn"
})
@ActiveProfiles("prod")
@Testcontainers
class ProdSeedProfileTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("MOOC")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("SPRING_DATASOURCE_URL", POSTGRES::getJdbcUrl);
        registry.add("SPRING_DATASOURCE_USERNAME", POSTGRES::getUsername);
        registry.add("SPRING_DATASOURCE_PASSWORD", POSTGRES::getPassword);
    }

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserAccountRepository userAccountRepository;

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
    private RoleRepository roleRepository;

    @Test
    void shouldBootWithReferenceRolesButNoDemoData() {
        assertThat(environment.getActiveProfiles())
                .contains("prod")
                .doesNotContain("demo", "test", "local");

        assertThat(applicationContext.getBeansOfType(DemoDataSeeder.class))
                .as("the demo seeder must not be loaded in production")
                .isEmpty();

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success", Integer.class))
                .isEqualTo(1);

        assertThat(environment.getProperty("springdoc.api-docs.enabled", Boolean.class))
                .as("production must not expose the full API inventory")
                .isFalse();
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isFalse();

        assertThat(roleRepository.count()).isEqualTo(3);
        assertThat(userAccountRepository.count()).isZero();
        assertThat(courseRepository.count()).isZero();
        assertThat(enrollmentRepository.count()).isZero();
        assertThat(collegeRepository.count()).isZero();
        assertThat(departmentRepository.count()).isZero();
        assertThat(affiliationRequestRepository.count()).isZero();
    }
}
