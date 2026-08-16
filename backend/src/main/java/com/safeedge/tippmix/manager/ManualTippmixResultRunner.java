package com.safeedge.tippmix.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("manual-results")
class ManualTippmixResultRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ManualTippmixResultRunner.class);

	private final TippmixResultIngestionManager ingestionManager;

	ManualTippmixResultRunner(TippmixResultIngestionManager ingestionManager) {
		this.ingestionManager = ingestionManager;
	}

	@Override
	public void run(ApplicationArguments args) {
		log.info("Starting manual Tippmix result collection run");
		ingestionManager.collectResults();
	}

}
