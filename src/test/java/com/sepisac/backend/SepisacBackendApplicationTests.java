package com.sepisac.backend;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SepisacBackendApplicationTests {

	@Autowired
	private Flyway flyway;

	@Test
	void contextLoads() {
	}

	@Test
	void flywayShouldBeConfiguredAndUpToDate() {
		assertThat(flyway).isNotNull();
		assertThat(flyway.info().current()).isNotNull();
		assertThat(flyway.info().current().getState().isApplied()).isTrue();
		assertThat(flyway.info().pending()).isEmpty();
	}

}
