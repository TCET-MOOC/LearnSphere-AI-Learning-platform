package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Repository.RoleRepository;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

	@Test
	void contextLoads() {
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
