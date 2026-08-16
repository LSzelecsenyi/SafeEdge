package com.safeedge.historical.footballdata.importer;

import com.safeedge.historical.domain.HistoricalAhCoverageReport;
import com.safeedge.historical.service.HistoricalAhCoverageReportFormatter;
import com.safeedge.historical.service.HistoricalAhCoverageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("manual-historical-coverage")
class ManualHistoricalAhCoverageRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ManualHistoricalAhCoverageRunner.class);

	private final HistoricalAhCoverageService coverageService;

	ManualHistoricalAhCoverageRunner(HistoricalAhCoverageService coverageService) {
		this.coverageService = coverageService;
	}

	@Override
	public void run(ApplicationArguments args) {
		HistoricalAhCoverageReport report = coverageService.report();
		log.info("Historical AH coverage audit:\n{}", HistoricalAhCoverageReportFormatter.format(report));
	}
}
