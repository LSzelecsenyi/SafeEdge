package com.safeedge.tippmix.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.safeedge.tippmix.manager.ResultCollectionRunResult;
import com.safeedge.tippmix.manager.TippmixResultIngestionManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

class TippmixResultSchedulingTest {

	private final TippmixResultIngestionManager ingestionManager = mock(TippmixResultIngestionManager.class);

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(TippmixResultSchedulingConfiguration.class)
			.withBean(TippmixResultIngestionManager.class, () -> ingestionManager);

	@Test
	void schedulerIsNotRegisteredWhenResultsAreDisabled() {
		runner.withPropertyValues("safeedge.providers.tippmix.results.enabled=false")
				.run(context -> {
					assertThat(context).doesNotHaveBean(TippmixResultSchedulingConfiguration.class);
					assertThat(context).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
					verifyNoInteractions(ingestionManager);
				});
	}

	@Test
	void schedulerIsNotRegisteredByDefault() {
		runner.run(context -> {
			assertThat(context).doesNotHaveBean(TippmixResultSchedulingConfiguration.class);
			verifyNoInteractions(ingestionManager);
		});
	}

	@Test
	void schedulerIsRegisteredWhenResultsAreEnabled() {
		runner.withPropertyValues(
						"safeedge.providers.tippmix.results.enabled=true",
						"safeedge.providers.tippmix.results.fixed-delay=PT15M")
				.run(context -> {
					assertThat(context).hasSingleBean(TippmixResultSchedulingConfiguration.class);
					assertThat(context).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class);
					verifyNoInteractions(ingestionManager);
				});
	}

	@Test
	void scheduledFailureDoesNotPreventLaterRuns() {
		when(ingestionManager.collectResults())
				.thenThrow(new RuntimeException("Tippmix HTTP failure"))
				.thenReturn(new ResultCollectionRunResult(
						0, 0, 0, 0, 0, 0, 0, 0, Instant.parse("2026-08-16T12:00:00Z"), List.of()));
		TippmixResultSchedulingConfiguration scheduler =
				new TippmixResultSchedulingConfiguration(ingestionManager);

		scheduler.collectFinishedFootballResults();
		scheduler.collectFinishedFootballResults();

		verify(ingestionManager, times(2)).collectResults();
	}

}
