package com.safeedge.tippmix.config;

import com.safeedge.tippmix.manager.TippmixPrematchCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Registers Tippmix pre-match collection on a single-thread Spring scheduler.
 *
 * {@code @EnableScheduling} uses a pool size of 1 by default, and this job uses
 * {@code fixedDelay} so the next run starts only after the previous run plus the delay.
 * Combined, overlapping scheduled executions are not expected for a single instance.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "safeedge.providers.tippmix.collector", name = "enabled", havingValue = "true")
public class TippmixCollectorSchedulingConfiguration {

	private static final Logger log = LoggerFactory.getLogger(TippmixCollectorSchedulingConfiguration.class);

	private final TippmixPrematchCollector collector;

	TippmixCollectorSchedulingConfiguration(TippmixPrematchCollector collector) {
		this.collector = collector;
	}

	@Scheduled(
			initialDelayString = "${safeedge.providers.tippmix.collector.fixed-delay}",
			fixedDelayString = "${safeedge.providers.tippmix.collector.fixed-delay}")
	void collectPrematchFootball() {
		try {
			collector.collectPrematchFootball();
		}
		catch (RuntimeException ex) {
			log.error("Scheduled Tippmix pre-match collection run failed", ex);
		}
	}

}
