package com.safeedge.strategy;

import com.safeedge.bankroll.BankrollState;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure stake/accept decision from an already-evaluated opportunity.
 * Does not mutate bankroll or exposure and does not branch on preset identity.
 */
public final class StrategyEngine {

	private static final MathContext MATH = MathContext.DECIMAL128;

	private final GeneralizedKellyCalculator kellyCalculator = new GeneralizedKellyCalculator();

	public StrategyDecision decide(
			StrategyConfig config,
			BettingOpportunity opportunity,
			BankrollState bankroll,
			PortfolioExposure exposure) {
		if (config == null) {
			throw new StrategyException("Strategy config is required");
		}
		if (opportunity == null) {
			throw new StrategyException("Betting opportunity is required");
		}
		if (bankroll == null) {
			throw new StrategyException("Bankroll state is required");
		}
		if (exposure == null) {
			throw new StrategyException("Portfolio exposure is required");
		}
		BigDecimal expectedReturn = kellyCalculator.expectedReturnRate(
				opportunity.odds(), opportunity.settlementProbabilities());
		BigDecimal active = bankroll.activeBankroll();
		if (active.compareTo(BigDecimal.ZERO) == 0) {
			return rejected(StrategyDecisionStatus.REJECTED, expectedReturn, null, StrategyDecisionReason.NO_ACTIVE_BANKROLL);
		}
		if (opportunity.edge().compareTo(config.minimumEdge()) < 0) {
			return rejected(StrategyDecisionStatus.REJECTED, expectedReturn, null, StrategyDecisionReason.EDGE_BELOW_MINIMUM);
		}
		BigDecimal drawdown = bankroll.activeDrawdownRate();
		if (drawdown.compareTo(config.drawdownStopThreshold()) >= 0) {
			return rejected(StrategyDecisionStatus.PAUSED_DRAWDOWN, expectedReturn, null, StrategyDecisionReason.DRAWDOWN_STOP);
		}
		List<StrategyDecisionReason> reasons = new ArrayList<>();
		BigDecimal fullKelly = null;
		BigDecimal calculatedStake;
		if (config.stakingMode() == StakingMode.FRACTIONAL_KELLY) {
			if (expectedReturn.compareTo(BigDecimal.ZERO) <= 0) {
				return rejected(
						StrategyDecisionStatus.REJECTED,
						expectedReturn,
						BigDecimal.ZERO,
						StrategyDecisionReason.NON_POSITIVE_EXPECTED_RETURN);
			}
			fullKelly = kellyCalculator.fullKellyFraction(
					opportunity.odds(), opportunity.settlementProbabilities());
			if (fullKelly.compareTo(BigDecimal.ZERO) <= 0) {
				return rejected(
						StrategyDecisionStatus.REJECTED,
						expectedReturn,
						fullKelly,
						StrategyDecisionReason.NON_POSITIVE_EXPECTED_RETURN);
			}
			calculatedStake = active.multiply(fullKelly).multiply(config.kellyFraction());
		}
		else {
			calculatedStake = active.multiply(config.flatStakeRate());
		}
		BigDecimal maxStakeAmount = active.multiply(config.maxStakeRate());
		BigDecimal stake = calculatedStake;
		if (stake.compareTo(maxStakeAmount) > 0) {
			stake = maxStakeAmount;
			reasons.add(StrategyDecisionReason.MAX_STAKE_CAPPED);
		}
		if (drawdown.compareTo(config.drawdownReductionThreshold()) >= 0) {
			stake = stake.multiply(config.drawdownStakeMultiplier());
			reasons.add(StrategyDecisionReason.DRAWDOWN_REDUCTION_APPLIED);
		}
		else if (drawdown.compareTo(config.drawdownWarningThreshold()) >= 0) {
			reasons.add(StrategyDecisionReason.DRAWDOWN_WARNING);
		}
		BigDecimal matchRemaining = remaining(active.multiply(config.maxMatchExposure()), exposure.matchExposureAmount());
		BigDecimal leagueRemaining = remaining(active.multiply(config.maxLeagueExposure()), exposure.leagueExposureAmount());
		BigDecimal dailyRemaining = remaining(active.multiply(config.maxDailyExposure()), exposure.dailyExposureAmount());
		BigDecimal stakeBeforeExposure = stake;
		if (matchRemaining.compareTo(stakeBeforeExposure) < 0) {
			reasons.add(StrategyDecisionReason.MATCH_EXPOSURE_CAPPED);
		}
		if (leagueRemaining.compareTo(stakeBeforeExposure) < 0) {
			reasons.add(StrategyDecisionReason.LEAGUE_EXPOSURE_CAPPED);
		}
		if (dailyRemaining.compareTo(stakeBeforeExposure) < 0) {
			reasons.add(StrategyDecisionReason.DAILY_EXPOSURE_CAPPED);
		}
		stake = stake.min(matchRemaining).min(leagueRemaining).min(dailyRemaining);
		if (stake.compareTo(BigDecimal.ZERO) <= 0) {
			reasons.add(StrategyDecisionReason.NO_EXPOSURE_CAPACITY);
			return new StrategyDecision(
					StrategyDecisionStatus.REJECTED,
					BigDecimal.ZERO,
					fullKelly,
					BigDecimal.ZERO,
					expectedReturn,
					reasons);
		}
		return new StrategyDecision(
				StrategyDecisionStatus.ACCEPTED,
				stake,
				fullKelly,
				stake.divide(active, MATH),
				expectedReturn,
				reasons);
	}

	private static BigDecimal remaining(BigDecimal limit, BigDecimal current) {
		return limit.subtract(current).max(BigDecimal.ZERO);
	}

	private static StrategyDecision rejected(
			StrategyDecisionStatus status,
			BigDecimal expectedReturn,
			BigDecimal fullKelly,
			StrategyDecisionReason reason) {
		return new StrategyDecision(
				status, BigDecimal.ZERO, fullKelly, BigDecimal.ZERO, expectedReturn, List.of(reason));
	}

}
