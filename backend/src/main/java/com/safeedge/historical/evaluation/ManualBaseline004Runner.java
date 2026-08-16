package com.safeedge.historical.evaluation;

import com.safeedge.historical.diagnostics.SerieAReplicationReportFormatter;
import com.safeedge.historical.diagnostics.ThreeLeagueValidationReportFormatter;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.service.Baseline004DiagnosticsRun;
import com.safeedge.historical.service.Baseline004DiagnosticsService;
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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("manual-baseline-004")
class ManualBaseline004Runner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ManualBaseline004Runner.class);
	private static final BigDecimal DEFAULT_STARTING_BANKROLL = new BigDecimal("100000");
	private static final String SERIE_A_REPORT = "docs/results/baseline-004-serie-a.md";
	private static final String THREE_LEAGUE_REPORT = "docs/results/baseline-004-three-league-validation.md";

	private final Baseline004DiagnosticsService diagnosticsService;
	private final HistoricalBacktestProperties properties;
	private final ConfigurableApplicationContext context;
	private final StrategyPresetFactory presetFactory = new StrategyPresetFactory();

	ManualBaseline004Runner(
			Baseline004DiagnosticsService diagnosticsService,
			HistoricalBacktestProperties properties,
			ConfigurableApplicationContext context) {
		this.diagnosticsService = diagnosticsService;
		this.properties = properties;
		this.context = context;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (blank(properties.competition())
				|| blank(properties.trainingFromSeason())
				|| blank(properties.fromSeason())
				|| blank(properties.toSeason())
				|| blank(properties.quoteSource())) {
			log.warn(
					"manual-baseline-004 profile is active but SAFEEDGE_HISTORICAL_BACKTEST_COMPETITION / TRAINING_FROM_SEASON / FROM_SEASON / TO_SEASON / QUOTE_SOURCE are not set; skipping");
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
				"Starting Baseline 004 Serie A replication: competition={} trainFrom={} eval={}→{} HISTORICAL QUOTE SOURCE={} startingBankroll={} (not Tippmix, zero-tuning)",
				competition,
				trainingFromSeason,
				evaluationFromSeason,
				evaluationToSeason,
				quoteSource,
				startingBankroll);
		Baseline004DiagnosticsRun run =
				diagnosticsService.diagnose(request, startingBankroll, defaultStrategies(), maxAcceptedBets);
		log.info("\n{}", HistoricalBacktestReportFormatter.format(run.comparison()));
		Path serieAPath = resolveRepoRoot().resolve(SERIE_A_REPORT);
		Path threeLeaguePath = resolveRepoRoot().resolve(THREE_LEAGUE_REPORT);
		Files.createDirectories(serieAPath.getParent());
		Files.writeString(
				serieAPath,
				SerieAReplicationReportFormatter.format(
						run.coverage(), run.baselineReport(), run.edgeQualityReport(), run.comparison()),
				StandardCharsets.UTF_8);
		Files.writeString(
				threeLeaguePath,
				ThreeLeagueValidationReportFormatter.format(run.threeLeagueComparison()),
				StandardCharsets.UTF_8);
		log.info("Wrote Baseline 004 Serie A report to {}", serieAPath.toAbsolutePath());
		log.info("Wrote Baseline 004 three-league report to {}", threeLeaguePath.toAbsolutePath());
		log.info("Spearman(SA)={}", run.edgeQualityReport().rankQuality().spearman());
		log.info("Classification={}", run.threeLeagueComparison().classification());
		context.close();
	}

	private List<NamedStrategyConfig> defaultStrategies() {
		return List.of(
				new NamedStrategyConfig("DEFENSIVE", presetFactory.configFor(StrategyPreset.DEFENSIVE)),
				new NamedStrategyConfig("BALANCED", presetFactory.configFor(StrategyPreset.BALANCED)),
				new NamedStrategyConfig("GROWTH", presetFactory.configFor(StrategyPreset.GROWTH)),
				new NamedStrategyConfig("FLAT_STAKE", presetFactory.configFor(StrategyPreset.FLAT_STAKE)));
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
