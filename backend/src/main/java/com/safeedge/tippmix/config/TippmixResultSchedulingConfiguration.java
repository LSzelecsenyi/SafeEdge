package com.safeedge.tippmix.config;

import com.safeedge.tippmix.manager.TippmixResultIngestionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Registers Tippmix result collection on a single-thread Spring scheduler.
 *
 * Independent from pre-match offer collection. {@code @EnableScheduling} uses a pool
 * size of 1 by default, and this job uses {@code fixedDelay} so the next run starts
 * only after the previous run plus the delay. The ingestion manager also guards against
 * overlapping triggers with an {@code AtomicBoolean}.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "safeedge.providers.tippmix.results", name = "enabled", havingValue = "true")
public class TippmixResultSchedulingConfiguration {

	private static final Logger log = LoggerFactory.getLogger(TippmixResultSchedulingConfiguration.class);

	private final TippmixResultIngestionManager ingestionManager;

	TippmixResultSchedulingConfiguration(TippmixResultIngestionManager ingestionManager) {
		this.ingestionManager = ingestionManager;
	}

	@Scheduled(
			initialDelayString = "${safeedge.providers.tippmix.results.fixed-delay}",
			fixedDelayString = "${safeedge.providers.tippmix.results.fixed-delay}")
	void collectFinishedFootballResults() {
		try {
			ingestionManager.collectResults();
		}
		catch (RuntimeException ex) {
			log.error("Scheduled Tippmix result collection run failed", ex);
		}
	}

}
