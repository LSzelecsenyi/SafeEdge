package com.safeedge.historical.evaluation;

import com.safeedge.historical.diagnostics.ProbabilityModelDevelopmentLeagues;
import com.safeedge.historical.diagnostics.ProbabilityModelV3DevelopmentReport;
import com.safeedge.historical.diagnostics.ProbabilityModelV3ReportFormatter;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.service.ProbabilityModelV3DevelopmentService;
import com.safeedge.probability.ProbabilityModelConfig;
import com.safeedge.probability.ProbabilityModelV3Config;
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
@Profile("manual-probability-model-v3")
class ManualProbabilityModelV3Runner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ManualProbabilityModelV3Runner.class);
	private static final BigDecimal STARTING_BANKROLL = new BigDecimal("100000");
	private static final String REPORT = "docs/results/probability-model-v3-development.md";
	private static final int TRAINING_FROM = 2014;
	private static final int EVAL_FROM = 2019;
	private static final int EVAL_TO = 2023;

	private final ProbabilityModelV3DevelopmentService developmentService;
	private final HistoricalBacktestProperties properties;
	private final ConfigurableApplicationContext context;
	private final StrategyPresetFactory presetFactory = new StrategyPresetFactory();

	ManualProbabilityModelV3Runner(
			ProbabilityModelV3DevelopmentService developmentService,
			HistoricalBacktestProperties properties,
			ConfigurableApplicationContext context) {
		this.developmentService = developmentService;
		this.properties = properties;
		this.context = context;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (!blank(properties.competition())) {
			CanonicalCompetition requested = CanonicalCompetition.valueOf(properties.competition().trim());
			ProbabilityModelDevelopmentLeagues.requireDevelopment(requested);
		}
		WalkForwardEvaluationRequest template = new WalkForwardEvaluationRequest(
				CanonicalCompetition.PREMIER_LEAGUE,
				TRAINING_FROM,
				EVAL_FROM,
				EVAL_TO,
				HistoricalQuoteSource.MARKET_AVERAGE,
				ProbabilityModelConfig.defaults());
		ProbabilityModelV3Config v3Config = ProbabilityModelV3Config.defaults();
		log.info(
				"Starting Probability Model v3 development evaluation on PREMIER_LEAGUE, BUNDESLIGA, SERIE_A; "
						+ "trainFrom={} eval={}→{} MARKET_AVERAGE; attackReg={} defenceReg={}; reserved leagues not run",
				TRAINING_FROM,
				EVAL_FROM,
				EVAL_TO,
				v3Config.attackRegularization(),
				v3Config.defenceRegularization());
		ProbabilityModelV3DevelopmentReport report = developmentService.evaluateDevelopmentLeagues(
				template, v3Config, STARTING_BANKROLL, defaultStrategies(), null);
		Path reportPath = resolveRepoRoot().resolve(REPORT);
		Files.createDirectories(reportPath.getParent());
		Files.writeString(reportPath, ProbabilityModelV3ReportFormatter.format(report), StandardCharsets.UTF_8);
		log.info("Wrote Probability Model v3 development report to {}", reportPath.toAbsolutePath());
		log.info("Classification={}", report.classification());
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
