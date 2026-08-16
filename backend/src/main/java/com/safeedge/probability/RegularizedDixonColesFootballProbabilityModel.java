package com.safeedge.probability;

import com.safeedge.candidate.ScoreProbabilityDistribution;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Probability Model v2: time-decayed Poisson with attack/defence shrinkage and
 * optional Dixon-Coles low-score dependence. v1
 * {@link PoissonFootballProbabilityModel} is unchanged.
 *
 * Rho is fitted from weighted historical score likelihood only. Market odds and
 * betting ROI never enter fitting.
 */
public final class RegularizedDixonColesFootballProbabilityModel implements FootballProbabilityModel {

	private static final MathContext MATH = MathContext.DECIMAL128;
	private static final BigDecimal ZERO = BigDecimal.ZERO;

	private final ProbabilityModelV2Config config;
	private final DixonColesFitRecorder recorder;

	public RegularizedDixonColesFootballProbabilityModel() {
		this(ProbabilityModelV2Config.defaults(), null);
	}

	public RegularizedDixonColesFootballProbabilityModel(ProbabilityModelV2Config config) {
		this(config, null);
	}

	public RegularizedDixonColesFootballProbabilityModel(
			ProbabilityModelV2Config config, DixonColesFitRecorder recorder) {
		if (config == null) {
			throw new ProbabilityModelException("config is required");
		}
		this.config = config;
		this.recorder = recorder;
	}

	public ProbabilityModelV2Config config() {
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
		WeightedTotals leagueHome = new WeightedTotals();
		WeightedTotals leagueAway = new WeightedTotals();
		Map<String, WeightedTotals> scoredAtHome = new LinkedHashMap<>();
		Map<String, WeightedTotals> concededAtHome = new LinkedHashMap<>();
		Map<String, WeightedTotals> scoredAway = new LinkedHashMap<>();
		Map<String, WeightedTotals> concededAway = new LinkedHashMap<>();
		List<EligibleMatch> eligibleMatches = new ArrayList<>();
		int eligible = 0;
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
			eligible++;
			double weight = TimeDecay.weight(
					ChronoUnit.DAYS.between(match.matchDate(), target.matchDate()), config.decayHalfLifeDays());
			leagueHome.add(weight, match.score().homeGoals());
			leagueAway.add(weight, match.score().awayGoals());
			totals(scoredAtHome, match.homeTeam()).add(weight, match.score().homeGoals());
			totals(concededAtHome, match.homeTeam()).add(weight, match.score().awayGoals());
			totals(scoredAway, match.awayTeam()).add(weight, match.score().awayGoals());
			totals(concededAway, match.awayTeam()).add(weight, match.score().homeGoals());
			eligibleMatches.add(new EligibleMatch(match, weight));
		}
		WeightedTotals homeScoredAtHome = totals(scoredAtHome, target.homeTeam());
		WeightedTotals awayScoredAway = totals(scoredAway, target.awayTeam());
		if (leagueHome.count == 0) {
			return ProbabilityPrediction.unavailable(
					ProbabilityPredictionStatus.NO_LEAGUE_HISTORY,
					eligible,
					homeScoredAtHome.count,
					awayScoredAway.count);
		}
		if (homeScoredAtHome.count < config.minimumTeamMatches()
				|| awayScoredAway.count < config.minimumTeamMatches()) {
			return ProbabilityPrediction.unavailable(
					ProbabilityPredictionStatus.INSUFFICIENT_HISTORY,
					eligible,
					homeScoredAtHome.count,
					awayScoredAway.count);
		}
		BigDecimal leagueHomeRate = leagueHome.average();
		BigDecimal leagueAwayRate = leagueAway.average();
		if (leagueHomeRate.compareTo(ZERO) == 0 || leagueAwayRate.compareTo(ZERO) == 0) {
			throw new ProbabilityModelException(
					"league scoring rate is zero; cannot compute attack/defence strength");
		}
		BigDecimal prior = config.attackDefenceShrinkageStrength();
		WeightedTotals homeConcededAtHome = totals(concededAtHome, target.homeTeam());
		WeightedTotals awayConcededAway = totals(concededAway, target.awayTeam());
		BigDecimal rawHomeAttack = strength(homeScoredAtHome, leagueHomeRate, ZERO);
		BigDecimal rawHomeDefence = strength(homeConcededAtHome, leagueAwayRate, ZERO);
		BigDecimal rawAwayAttack = strength(awayScoredAway, leagueAwayRate, ZERO);
		BigDecimal rawAwayDefence = strength(awayConcededAway, leagueHomeRate, ZERO);
		BigDecimal homeAttack = strength(homeScoredAtHome, leagueHomeRate, prior);
		BigDecimal homeDefence = strength(homeConcededAtHome, leagueAwayRate, prior);
		BigDecimal awayAttack = strength(awayScoredAway, leagueAwayRate, prior);
		BigDecimal awayDefence = strength(awayConcededAway, leagueHomeRate, prior);
		if (homeAttack.compareTo(ZERO) < 0
				|| homeDefence.compareTo(ZERO) < 0
				|| awayAttack.compareTo(ZERO) < 0
				|| awayDefence.compareTo(ZERO) < 0) {
			throw new ProbabilityModelException("team strength must be >= 0");
		}
		BigDecimal lambdaHome = leagueHomeRate.multiply(homeAttack, MATH).multiply(awayDefence, MATH);
		BigDecimal lambdaAway = leagueAwayRate.multiply(awayAttack, MATH).multiply(homeDefence, MATH);
		if (lambdaHome.compareTo(ZERO) < 0
				|| lambdaAway.compareTo(ZERO) < 0
				|| !Double.isFinite(lambdaHome.doubleValue())
				|| !Double.isFinite(lambdaAway.doubleValue())) {
			throw new ProbabilityModelException("lambda must be finite and >= 0");
		}
		double rho = 0.0d;
		int rhoObservations = 0;
		if (config.dixonColesEnabled()) {
			List<DixonColesRhoFitter.RhoObservation> observations = rhoObservations(
					eligibleMatches, scoredAtHome, concededAtHome, scoredAway, concededAway, leagueHomeRate, leagueAwayRate, prior);
			rhoObservations = observations.size();
			rho = DixonColesRhoFitter.fit(observations);
		}
		double[] homePmf = IndependentPoisson.pmf(lambdaHome.doubleValue(), config.maxGoalsPerTeam());
		double[] awayPmf = IndependentPoisson.pmf(lambdaAway.doubleValue(), config.maxGoalsPerTeam());
		double[][] joint = IndependentPoisson.joint(homePmf, awayPmf);
		if (config.dixonColesEnabled()) {
			DixonColesTau.applyToJoint(joint, lambdaHome.doubleValue(), lambdaAway.doubleValue(), rho);
		}
		double captured = IndependentPoisson.sum(joint);
		ScoreProbabilityDistribution distribution =
				ScoreGridNormalizer.normalize(joint, captured, lambdaHome, lambdaAway);
		if (recorder != null) {
			recorder.record(new DixonColesFitSnapshot(
					target.matchDate(),
					rawHomeAttack,
					rawHomeDefence,
					rawAwayAttack,
					rawAwayDefence,
					homeAttack,
					homeDefence,
					awayAttack,
					awayDefence,
					lambdaHome,
					lambdaAway,
					BigDecimal.valueOf(rho),
					rhoObservations));
		}
		return ProbabilityPrediction.available(
				lambdaHome,
				lambdaAway,
				distribution,
				eligible,
				homeScoredAtHome.count,
				awayScoredAway.count,
				BigDecimal.valueOf(captured));
	}

	/**
	 * Rho is fitted from weighted score likelihood of matches with
	 * {@code matchDate < targetDate}. Lambdas use the same as-of-T shrunk
	 * strengths as the target (not a nested walk-forward per historical match).
	 * Market odds never enter.
	 */
	private List<DixonColesRhoFitter.RhoObservation> rhoObservations(
			List<EligibleMatch> matches,
			Map<String, WeightedTotals> scoredAtHome,
			Map<String, WeightedTotals> concededAtHome,
			Map<String, WeightedTotals> scoredAway,
			Map<String, WeightedTotals> concededAway,
			BigDecimal leagueHomeRate,
			BigDecimal leagueAwayRate,
			BigDecimal prior) {
		List<DixonColesRhoFitter.RhoObservation> observations = new ArrayList<>();
		boolean requireExposure = prior.compareTo(ZERO) == 0;
		for (EligibleMatch eligible : matches) {
			ProbabilityTrainingMatch match = eligible.match();
			WeightedTotals homeAtt = totals(scoredAtHome, match.homeTeam());
			WeightedTotals homeDef = totals(concededAtHome, match.homeTeam());
			WeightedTotals awayAtt = totals(scoredAway, match.awayTeam());
			WeightedTotals awayDef = totals(concededAway, match.awayTeam());
			if (requireExposure
					&& (homeAtt.count == 0 || homeDef.count == 0 || awayAtt.count == 0 || awayDef.count == 0)) {
				continue;
			}
			double lambdaHome = leagueHomeRate
					.multiply(strength(homeAtt, leagueHomeRate, prior), MATH)
					.multiply(strength(awayDef, leagueHomeRate, prior), MATH)
					.doubleValue();
			double lambdaAway = leagueAwayRate
					.multiply(strength(awayAtt, leagueAwayRate, prior), MATH)
					.multiply(strength(homeDef, leagueAwayRate, prior), MATH)
					.doubleValue();
			if (!Double.isFinite(lambdaHome)
					|| !Double.isFinite(lambdaAway)
					|| lambdaHome < 0.0d
					|| lambdaAway < 0.0d) {
				continue;
			}
			int homeGoals = match.score().homeGoals();
			int awayGoals = match.score().awayGoals();
			double independent = IndependentPoisson.probability(lambdaHome, homeGoals)
					* IndependentPoisson.probability(lambdaAway, awayGoals);
			if (!(independent > 0.0d) || !Double.isFinite(independent)) {
				continue;
			}
			observations.add(new DixonColesRhoFitter.RhoObservation(
					eligible.weight(), lambdaHome, lambdaAway, homeGoals, awayGoals, independent));
		}
		return observations;
	}

	private static BigDecimal strength(WeightedTotals totals, BigDecimal leagueRate, BigDecimal prior) {
		if (totals.count == 0 && prior.compareTo(ZERO) == 0) {
			throw new ProbabilityModelException("cannot shrink a team with zero exposure and zero prior");
		}
		if (totals.count == 0) {
			return AttackDefenceShrinkage.shrunkStrength(ZERO, ZERO, leagueRate, prior);
		}
		return AttackDefenceShrinkage.shrunkStrength(totals.weightedSum, totals.weightTotal, leagueRate, prior);
	}

	private static WeightedTotals totals(Map<String, WeightedTotals> byTeam, String team) {
		return byTeam.computeIfAbsent(team, key -> new WeightedTotals());
	}

	private static final class EligibleMatch {
		private final ProbabilityTrainingMatch match;
		private final double weight;

		private EligibleMatch(ProbabilityTrainingMatch match, double weight) {
			this.match = match;
			this.weight = weight;
		}

		private ProbabilityTrainingMatch match() {
			return match;
		}

		private double weight() {
			return weight;
		}
	}

	private static final class WeightedTotals {
		private BigDecimal weightedSum = ZERO;
		private BigDecimal weightTotal = ZERO;
		private int count;

		private void add(double weight, int goals) {
			BigDecimal w = BigDecimal.valueOf(weight);
			weightedSum = weightedSum.add(w.multiply(BigDecimal.valueOf(goals)));
			weightTotal = weightTotal.add(w);
			count++;
		}

		private BigDecimal average() {
			if (count == 0) {
				throw new ProbabilityModelException("cannot average an empty history");
			}
			return weightedSum.divide(weightTotal, MATH);
		}
	}
}
