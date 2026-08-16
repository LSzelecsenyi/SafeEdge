package com.safeedge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(PostgresTestcontainersConfig.class)
@SpringBootTest
class SafeEdgeApplicationTests {

	@Test
	void contextLoads() {
	}

}
