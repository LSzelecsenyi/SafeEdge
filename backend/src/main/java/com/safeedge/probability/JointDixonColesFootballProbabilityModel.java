package com.safeedge.probability;

import com.safeedge.candidate.ScoreProbabilityDistribution;
import com.safeedge.historical.domain.CanonicalCompetition;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Probability Model v3: jointly fitted regularized Dixon-Coles team strengths.
 * v1 {@link PoissonFootballProbabilityModel} and v2
 * {@link RegularizedDixonColesFootballProbabilityModel} are unchanged.
 *
 * Defence convention: positive defence is stronger (subtracted on the log-link).
 * Identifiability: centered attack and defence. Rho is jointly fitted from
 * score likelihood only. Market odds never enter.
 *
 * Same-date cache reuses a fit for one cutoff. Warm-start uses only earlier
 * cutoffs as initial values.
 */
public final class JointDixonColesFootballProbabilityModel implements FootballProbabilityModel {

	private final ProbabilityModelV3Config config;
	private final JointDixonColesFitRecorder recorder;
	private CachedFit dateCache;
	private CachedFit lastEarlierFit;

	public JointDixonColesFootballProbabilityModel() {
		this(ProbabilityModelV3Config.defaults(), null);
	}

	public JointDixonColesFootballProbabilityModel(ProbabilityModelV3Config config) {
		this(config, null);
	}

	public JointDixonColesFootballProbabilityModel(
			ProbabilityModelV3Config config, JointDixonColesFitRecorder recorder) {
		if (config == null) {
			throw new ProbabilityModelException("config is required");
		}
		this.config = config;
		this.recorder = recorder;
	}

	public ProbabilityModelV3Config config() {
		return config;
	}

	@Override
	public ProbabilityPrediction predict(List<ProbabilityTrainingMatch> trainingData, MatchPredictionContext target) {
		if (trainingData == null) {
			throw new ProbabilityModelException("trainingData is required");
		}
		if (target == null) {
			throw new ProbabilityModelException("target is required");
		}
		List<EligibleMatch> eligible = new ArrayList<>();
		Map<String, Integer> teamMatches = new LinkedHashMap<>();
		double weightedHomeGoals = 0.0d;
		double weightedAwayGoals = 0.0d;
		double weightTotal = 0.0d;
		for (ProbabilityTrainingMatch match : trainingData) {
			if (match == null) {
				throw new ProbabilityModelException("trainingData must not contain null");
			}
			if (match.competition() != target.competition()) {
				continue;
			}
			if (!match.matchDate().isBefore(target.matchDate())) {
				continue;
			}
			double weight = TimeDecay.weight(
					ChronoUnit.DAYS.between(match.matchDate(), target.matchDate()), config.decayHalfLifeDays());
			eligible.add(new EligibleMatch(match, weight));
			teamMatches.merge(match.homeTeam(), 1, Integer::sum);
			teamMatches.merge(match.awayTeam(), 1, Integer::sum);
			weightedHomeGoals += weight * match.score().homeGoals();
			weightedAwayGoals += weight * match.score().awayGoals();
			weightTotal += weight;
		}
		int homeHistory = teamMatches.getOrDefault(target.homeTeam(), 0);
		int awayHistory = teamMatches.getOrDefault(target.awayTeam(), 0);
		if (eligible.isEmpty() || weightTotal <= 0.0d) {
			return ProbabilityPrediction.unavailable(
					ProbabilityPredictionStatus.NO_LEAGUE_HISTORY, eligible.size(), homeHistory, awayHistory);
		}
		if (eligible.size() < config.minimumLeagueMatches()
				|| homeHistory < config.minimumTeamMatches()
				|| awayHistory < config.minimumTeamMatches()) {
			return ProbabilityPrediction.unavailable(
					ProbabilityPredictionStatus.INSUFFICIENT_HISTORY, eligible.size(), homeHistory, awayHistory);
		}
		JointDixonColesFit fit = fit(target, eligible, weightedHomeGoals / weightTotal, weightedAwayGoals / weightTotal);
		if (fit == null || !fit.success()) {
			if (recorder != null) {
				recorder.recordFailure();
			}
			return ProbabilityPrediction.unavailable(
					ProbabilityPredictionStatus.FITTING_FAILED, eligible.size(), homeHistory, awayHistory);
		}
		double lambdaHome = fit.lambdaHome(target.homeTeam(), target.awayTeam());
		double lambdaAway = fit.lambdaAway(target.homeTeam(), target.awayTeam());
		if (!Double.isFinite(lambdaHome)
				|| !Double.isFinite(lambdaAway)
				|| lambdaHome <= 0.0d
				|| lambdaAway <= 0.0d
				|| !DixonColesTau.validFor(lambdaHome, lambdaAway, fit.rho())) {
			if (recorder != null) {
				recorder.recordFailure();
			}
			return ProbabilityPrediction.unavailable(
					ProbabilityPredictionStatus.FITTING_FAILED, eligible.size(), homeHistory, awayHistory);
		}
		BigDecimal home = BigDecimal.valueOf(lambdaHome);
		BigDecimal away = BigDecimal.valueOf(lambdaAway);
		double[] homePmf = IndependentPoisson.pmf(lambdaHome, config.maxGoalsPerTeam());
		double[] awayPmf = IndependentPoisson.pmf(lambdaAway, config.maxGoalsPerTeam());
		double[][] joint = IndependentPoisson.joint(homePmf, awayPmf);
		DixonColesTau.applyToJoint(joint, lambdaHome, lambdaAway, fit.rho());
		double captured = IndependentPoisson.sum(joint);
		ScoreProbabilityDistribution distribution = ScoreGridNormalizer.normalize(joint, captured, home, away);
		if (recorder != null) {
			int homeIdx = fit.teamIndex(target.homeTeam());
			int awayIdx = fit.teamIndex(target.awayTeam());
			recorder.record(new JointDixonColesFitSnapshot(
					target.matchDate(),
					BigDecimal.valueOf(fit.intercept()),
					BigDecimal.valueOf(fit.homeAdvantage()),
					BigDecimal.valueOf(fit.attack()[homeIdx]),
					BigDecimal.valueOf(fit.defence()[homeIdx]),
					BigDecimal.valueOf(fit.attack()[awayIdx]),
					BigDecimal.valueOf(fit.defence()[awayIdx]),
					home,
					away,
					BigDecimal.valueOf(fit.rho()),
					fit.iterations(),
					fit.converged(),
					fit.teams().size(),
					eligible.size()));
		}
		return ProbabilityPrediction.available(home, away, distribution, eligible.size(), homeHistory, awayHistory, BigDecimal.valueOf(captured));
	}

	private JointDixonColesFit fit(
			MatchPredictionContext target, List<EligibleMatch> eligible, double leagueHomeRate, double leagueAwayRate) {
		long fingerprint = fingerprint(eligible);
		if (dateCache != null
				&& dateCache.competition() == target.competition()
				&& dateCache.cutoff().equals(target.matchDate())
				&& dateCache.fingerprint() == fingerprint) {
			return dateCache.fit();
		}
		TreeSet<String> names = new TreeSet<>();
		for (EligibleMatch match : eligible) {
			names.add(match.match().homeTeam());
			names.add(match.match().awayTeam());
		}
		List<String> teams = List.copyOf(names);
		Map<String, Integer> index = new LinkedHashMap<>();
		for (int i = 0; i < teams.size(); i++) {
			index.put(teams.get(i), i);
		}
		List<JointDixonColesFitter.FitObservation> observations = new ArrayList<>(eligible.size());
		for (EligibleMatch match : eligible) {
			observations.add(new JointDixonColesFitter.FitObservation(
					match.weight(),
					index.get(match.match().homeTeam()),
					index.get(match.match().awayTeam()),
					match.match().score().homeGoals(),
					match.match().score().awayGoals()));
		}
		JointDixonColesFit warmStart = null;
		if (lastEarlierFit != null && lastEarlierFit.cutoff().isBefore(target.matchDate())) {
			warmStart = lastEarlierFit.fit();
		}
		JointDixonColesFit fitted =
				JointDixonColesFitter.fit(teams, observations, leagueHomeRate, leagueAwayRate, config, warmStart);
		dateCache = new CachedFit(target.competition(), target.matchDate(), fingerprint, fitted);
		if (fitted.success()
				&& (lastEarlierFit == null || lastEarlierFit.cutoff().isBefore(target.matchDate()))) {
			lastEarlierFit = dateCache;
		}
		return fitted;
	}

	private static long fingerprint(List<EligibleMatch> matches) {
		long hash = matches.size();
		for (EligibleMatch match : matches) {
			hash = 31 * hash + match.match().matchDate().hashCode();
			hash = 31 * hash + match.match().homeTeam().hashCode();
			hash = 31 * hash + match.match().awayTeam().hashCode();
			hash = 31 * hash + match.match().score().homeGoals();
			hash = 31 * hash + 17L * match.match().score().awayGoals();
		}
		return hash;
	}

	private record EligibleMatch(ProbabilityTrainingMatch match, double weight) {
	}

	private record CachedFit(
			CanonicalCompetition competition, LocalDate cutoff, long fingerprint, JointDixonColesFit fit) {
	}
}
