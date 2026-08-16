package com.safeedge.historical.evaluation;

import com.safeedge.historical.diagnostics.EdgeQualityReportFormatter;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.service.EdgeQualityDiagnosticsRun;
import com.safeedge.historical.service.EdgeQualityDiagnosticsService;
import com.safeedge.probability.ProbabilityModelConfig;
import com.safeedge.strategy.StrategyPreset;
import com.safeedge.strategy.StrategyPresetFactory;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("manual-baseline-002")
class ManualBaseline002Runner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ManualBaseline002Runner.class);
	private static final BigDecimal DEFAULT_STARTING_BANKROLL = new BigDecimal("100000");
	private static final String REPORT_RELATIVE = "docs/results/baseline-002-edge-quality.md";

	private final EdgeQualityDiagnosticsService diagnosticsService;
	private final HistoricalBacktestProperties properties;
	private final StrategyPresetFactory presetFactory = new StrategyPresetFactory();

	ManualBaseline002Runner(
			EdgeQualityDiagnosticsService diagnosticsService, HistoricalBacktestProperties properties) {
		this.diagnosticsService = diagnosticsService;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (blank(properties.competition())
				|| blank(properties.trainingFromSeason())
				|| blank(properties.fromSeason())
				|| blank(properties.toSeason())
				|| blank(properties.quoteSource())) {
			log.warn(
					"manual-baseline-002 profile is active but SAFEEDGE_HISTORICAL_BACKTEST_COMPETITION / TRAINING_FROM_SEASON / FROM_SEASON / TO_SEASON / QUOTE_SOURCE are not set; skipping");
			return;
		}
		CanonicalCompetition competition = CanonicalCompetition.valueOf(properties.competition().trim());
		int trainingFromSeason = Integer.parseInt(properties.trainingFromSeason().trim());
		int evaluationFromSeason = Integer.parseInt(properties.fromSeason().trim());
		int evaluationToSeason = Integer.parseInt(properties.toSeason().trim());
		HistoricalQuoteSource quoteSource = HistoricalQuoteSource.valueOf(properties.quoteSource().trim());
		WalkForwardEvaluationRequest request = new WalkForwardEvaluationRequest(
				competition,
				trainingFromSeason,
				evaluationFromSeason,
				evaluationToSeason,
				quoteSource,
				ProbabilityModelConfig.defaults());
		BigDecimal startingBankroll = blank(properties.startingBankroll())
				? DEFAULT_STARTING_BANKROLL
				: new BigDecimal(properties.startingBankroll().trim());
		Integer maxAcceptedBets = blank(properties.maxAcceptedBets())
				? null
				: Integer.valueOf(properties.maxAcceptedBets().trim());
		log.info(
				"Starting Baseline 002 edge-quality diagnostics: competition={} trainFrom={} eval={}→{} HISTORICAL QUOTE SOURCE={} startingBankroll={} (not Tippmix)",
				competition,
				trainingFromSeason,
				evaluationFromSeason,
				evaluationToSeason,
				quoteSource,
				startingBankroll);
		EdgeQualityDiagnosticsRun run =
				diagnosticsService.diagnose(request, startingBankroll, defaultStrategies(), maxAcceptedBets);
		log.info("\n{}", HistoricalBacktestReportFormatter.format(run.comparison()));
		String markdown = EdgeQualityReportFormatter.format(run.report());
		Path reportPath = reportPath();
		Files.createDirectories(reportPath.getParent());
		Files.writeString(reportPath, markdown, StandardCharsets.UTF_8);
		log.info("Wrote Baseline 002 report to {}", reportPath.toAbsolutePath());
		log.info("Spearman={}", run.report().rankQuality().spearman());
	}

	private List<NamedStrategyConfig> defaultStrategies() {
		return List.of(
				new NamedStrategyConfig("DEFENSIVE", presetFactory.configFor(StrategyPreset.DEFENSIVE)),
				new NamedStrategyConfig("BALANCED", presetFactory.configFor(StrategyPreset.BALANCED)),
				new NamedStrategyConfig("GROWTH", presetFactory.configFor(StrategyPreset.GROWTH)),
				new NamedStrategyConfig("FLAT_STAKE", presetFactory.configFor(StrategyPreset.FLAT_STAKE)));
	}

	static Path reportPath() {
		return resolveRepoRoot().resolve(REPORT_RELATIVE);
	}

	private static Path resolveRepoRoot() {
		Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
		if (Files.isDirectory(cwd.resolve("docs")) && Files.isDirectory(cwd.resolve("backend"))) {
			return cwd;
		}
		Path parent = cwd.getParent();
		if (parent != null && Files.isDirectory(parent.resolve("docs"))) {
			return parent;
		}
		return cwd;
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}
}
