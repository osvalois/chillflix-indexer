package com.chillflix.indexer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Disabled until database configuration for tests is fixed")
class IndexerApplicationTests {

	@Test
	void contextLoads() {
		// Empty test that verifies the application context loads successfully
	}

}
