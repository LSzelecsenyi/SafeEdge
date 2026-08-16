package com.safeedge.tippmix.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("manual-collection")
class ManualTippmixCollectionRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ManualTippmixCollectionRunner.class);

	private final TippmixPrematchCollector collector;

	ManualTippmixCollectionRunner(TippmixPrematchCollector collector) {
		this.collector = collector;
	}

	@Override
	public void run(ApplicationArguments args) {
		log.info("Starting manual Tippmix pre-match collection run");
		collector.collectPrematchFootball();
	}

}
