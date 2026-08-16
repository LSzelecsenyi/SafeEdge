package com.safeedge.tippmix.manager;

import com.safeedge.tippmix.config.TippmixProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("manual-ingestion")
class ManualTippmixIngestionRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ManualTippmixIngestionRunner.class);

	private final TippmixIngestionManager ingestionManager;
	private final TippmixProperties properties;

	ManualTippmixIngestionRunner(TippmixIngestionManager ingestionManager, TippmixProperties properties) {
		this.ingestionManager = ingestionManager;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		String rawEventId = properties.manualEventId();
		if (rawEventId == null || rawEventId.isBlank()) {
			log.warn("manual-ingestion profile is active but TIPPMIX_MANUAL_EVENT_ID is not set; skipping");
			return;
		}
		long eventId = Long.parseLong(rawEventId.trim());
		log.info("Starting manual Tippmix ingestion for eventId={}", eventId);
		ingestionManager.ingestPrematchEvent(eventId);
	}

}
