package com.safeedge.tippmix.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.safeedge.tippmix.manager.TippmixPrematchCollector;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

class TippmixCollectorSchedulingTest {

	private final TippmixPrematchCollector collector = mock(TippmixPrematchCollector.class);

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(TippmixCollectorSchedulingConfiguration.class)
			.withBean(TippmixPrematchCollector.class, () -> collector);

	@Test
	void schedulerIsNotRegisteredWhenCollectorIsDisabled() {
		runner.withPropertyValues("safeedge.providers.tippmix.collector.enabled=false")
				.run(context -> {
					assertThat(context).doesNotHaveBean(TippmixCollectorSchedulingConfiguration.class);
					assertThat(context).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
					verifyNoInteractions(collector);
				});
	}

	@Test
	void schedulerIsNotRegisteredByDefault() {
		runner.run(context -> {
			assertThat(context).doesNotHaveBean(TippmixCollectorSchedulingConfiguration.class);
			verifyNoInteractions(collector);
		});
	}

	@Test
	void schedulerIsRegisteredWhenCollectorIsEnabled() {
		runner.withPropertyValues(
						"safeedge.providers.tippmix.collector.enabled=true",
						"safeedge.providers.tippmix.collector.fixed-delay=PT5M")
				.run(context -> {
					assertThat(context).hasSingleBean(TippmixCollectorSchedulingConfiguration.class);
					assertThat(context).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class);
					verifyNoInteractions(collector);
				});
	}

}
