package com.safeedge;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.tippmix.config.TippmixCollectorSchedulingConfiguration;
import com.safeedge.tippmix.config.TippmixResultSchedulingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

@Import(PostgresTestcontainersConfig.class)
@SpringBootTest(
		properties = {
			"safeedge.providers.tippmix.collector.enabled=false",
			"safeedge.providers.tippmix.results.enabled=false"
		})
class SafeEdgeApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		assertThat(applicationContext.getBeanNamesForType(TippmixCollectorSchedulingConfiguration.class)).isEmpty();
		assertThat(applicationContext.getBeanNamesForType(TippmixResultSchedulingConfiguration.class)).isEmpty();
	}

}
