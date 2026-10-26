package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Repository.RoleRepository;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
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

	@Test
	void contextLoads() {
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

}
