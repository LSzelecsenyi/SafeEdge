package com.safeedge.probability;

import com.safeedge.candidate.ScoreProbabilityDistribution;
import com.safeedge.historical.features.HistoricalMatchRecord;
import com.safeedge.historical.features.HistoricalModelRow;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Time-decayed independent Poisson score model. Dixon-Coles rho is not fitted
 * in v1; low-score dependence is a later upgrade.
 */
public final class PoissonFootballProbabilityModel implements FootballProbabilityModel {

	private static final MathContext MATH = MathContext.DECIMAL128;

	private final ProbabilityModelConfig config;

	public PoissonFootballProbabilityModel() {
		this(ProbabilityModelConfig.defaults());
	}

	public PoissonFootballProbabilityModel(ProbabilityModelConfig config) {
		if (config == null) {
			throw new ProbabilityModelException("config is required");
		}
		this.config = config;
	}

	public ProbabilityModelConfig config() {
		return config;
	}

	public ProbabilityPrediction predictFromModelRows(
			List<HistoricalModelRow> rows, MatchPredictionContext target) {
		if (rows == null) {
			throw new ProbabilityModelException("trainingData is required");
		}
		List<ProbabilityTrainingMatch> training = new ArrayList<>(rows.size());
		for (HistoricalModelRow row : rows) {
			if (row == null) {
				throw new ProbabilityModelException("trainingData must not contain null");
			}
			training.add(ProbabilityTrainingMatch.from(row));
		}
		return predict(training, target);
	}

	public ProbabilityPrediction predictFromRecords(
			List<HistoricalMatchRecord> records, MatchPredictionContext target) {
		if (records == null) {
			throw new ProbabilityModelException("trainingData is required");
		}
		List<ProbabilityTrainingMatch> training = new ArrayList<>(records.size());
		for (HistoricalMatchRecord record : records) {
			if (record == null) {
				throw new ProbabilityModelException("trainingData must not contain null");
			}
			training.add(ProbabilityTrainingMatch.from(record));
		}
		return predict(training, target);
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
		WeightedTotals homeScoredAtHome = new WeightedTotals();
		WeightedTotals homeConcededAtHome = new WeightedTotals();
		WeightedTotals awayScoredAway = new WeightedTotals();
		WeightedTotals awayConcededAway = new WeightedTotals();
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
			if (match.homeTeam().equals(target.homeTeam())) {
				homeScoredAtHome.add(weight, match.score().homeGoals());
				homeConcededAtHome.add(weight, match.score().awayGoals());
			}
			if (match.awayTeam().equals(target.awayTeam())) {
				awayScoredAway.add(weight, match.score().awayGoals());
				awayConcededAway.add(weight, match.score().homeGoals());
			}
		}
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
		if (leagueHomeRate.compareTo(BigDecimal.ZERO) == 0 || leagueAwayRate.compareTo(BigDecimal.ZERO) == 0) {
			throw new ProbabilityModelException(
					"league scoring rate is zero; cannot compute attack/defence strength");
		}
		BigDecimal homeAttack = homeScoredAtHome.average().divide(leagueHomeRate, MATH);
		BigDecimal homeDefence = homeConcededAtHome.average().divide(leagueAwayRate, MATH);
		BigDecimal awayAttack = awayScoredAway.average().divide(leagueAwayRate, MATH);
		BigDecimal awayDefence = awayConcededAway.average().divide(leagueHomeRate, MATH);
		BigDecimal lambdaHome = leagueHomeRate.multiply(homeAttack, MATH).multiply(awayDefence, MATH);
		BigDecimal lambdaAway = leagueAwayRate.multiply(awayAttack, MATH).multiply(homeDefence, MATH);
		double[] homePmf = IndependentPoisson.pmf(lambdaHome.doubleValue(), config.maxGoalsPerTeam());
		double[] awayPmf = IndependentPoisson.pmf(lambdaAway.doubleValue(), config.maxGoalsPerTeam());
		double[][] joint = IndependentPoisson.joint(homePmf, awayPmf);
		double captured = IndependentPoisson.sum(joint);
		ScoreProbabilityDistribution distribution = ScoreGridNormalizer.normalize(joint, captured);
		return ProbabilityPrediction.available(
				lambdaHome,
				lambdaAway,
				distribution,
				eligible,
				homeScoredAtHome.count,
				awayScoredAway.count,
				BigDecimal.valueOf(captured));
	}

	private static final class WeightedTotals {
		private BigDecimal weightedSum = BigDecimal.ZERO;
		private BigDecimal weightTotal = BigDecimal.ZERO;
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
