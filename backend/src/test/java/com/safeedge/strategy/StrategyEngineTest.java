package com.safeedge.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.bankroll.BankrollState;
import com.safeedge.bankroll.OwnerId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StrategyEngineTest {

	private static final OwnerId OWNER = new OwnerId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
	private static final LocalDate DATE = LocalDate.of(2026, 8, 16);

	private final StrategyEngine engine = new StrategyEngine();
	private final StrategyPresetFactory presets = new StrategyPresetFactory();

	@Test
	void pipelineCapsKellyThenDrawdownThenMatchExposure() {
		StrategyConfig config = defensiveLike();
		BankrollState bankroll = bankroll("100000", "0", "120000");
		assertThat(bankroll.activeDrawdownRate()).isGreaterThanOrEqualTo(config.drawdownReductionThreshold());
		assertThat(bankroll.activeDrawdownRate()).isLessThan(config.drawdownStopThreshold());
		PortfolioExposure exposure = new PortfolioExposure(money("2300"), BigDecimal.ZERO, BigDecimal.ZERO);
		StrategyDecision decision = engine.decide(config, binaryOpportunity("0.05"), bankroll, exposure);
		assertThat(decision.status()).isEqualTo(StrategyDecisionStatus.ACCEPTED);
		assertThat(decision.stake()).isEqualByComparingTo("700");
		assertThat(decision.reasons()).containsExactly(
				StrategyDecisionReason.MAX_STAKE_CAPPED,
				StrategyDecisionReason.DRAWDOWN_REDUCTION_APPLIED,
				StrategyDecisionReason.MATCH_EXPOSURE_CAPPED);
		assertThat(decision.fullKellyFraction()).isPositive();
	}

	@Test
	void equalMinimumEdgeIsEligible() {
		StrategyConfig config = defensiveLike();
		BettingOpportunity opportunity = binaryOpportunity("0.03");
		StrategyDecision decision = engine.decide(config, opportunity, bankroll("100000", "0", "100000"), PortfolioExposure.none());
		assertThat(decision.status()).isEqualTo(StrategyDecisionStatus.ACCEPTED);
		assertThat(decision.stake()).isPositive();
	}

	@Test
	void edgeBelowMinimumRejectsBeforeSizing() {
		StrategyDecision decision = engine.decide(
				defensiveLike(),
				binaryOpportunity("0.029"),
				bankroll("100000", "0", "100000"),
				PortfolioExposure.none());
		assertThat(decision.status()).isEqualTo(StrategyDecisionStatus.REJECTED);
		assertThat(decision.stake()).isEqualByComparingTo("0");
		assertThat(decision.reasons()).containsExactly(StrategyDecisionReason.EDGE_BELOW_MINIMUM);
		assertThat(decision.fullKellyFraction()).isNull();
	}

	@Test
	void drawdownBoundaries() {
		StrategyConfig config = defensiveLike();
		BettingOpportunity opportunity = binaryOpportunity("0.05");
		assertAcceptedWithout(config, bankroll("91000", "0", "100000"), StrategyDecisionReason.DRAWDOWN_WARNING);
		assertHasReason(config, opportunity, bankroll("90000", "0", "100000"), StrategyDecisionReason.DRAWDOWN_WARNING, false);
		assertHasReason(config, opportunity, bankroll("85001", "0", "100000"), StrategyDecisionReason.DRAWDOWN_WARNING, false);
		assertHasReason(config, opportunity, bankroll("85000", "0", "100000"), StrategyDecisionReason.DRAWDOWN_REDUCTION_APPLIED, true);
		assertHasReason(config, opportunity, bankroll("80001", "0", "100000"), StrategyDecisionReason.DRAWDOWN_REDUCTION_APPLIED, true);
		assertPaused(config, opportunity, bankroll("80000", "0", "100000"));
		assertPaused(config, opportunity, bankroll("79000", "0", "100000"));
	}

	@Test
	void exposureCapsApplyIndependentlyAndTogether() {
		StrategyConfig config = defensiveLike();
		BankrollState bankroll = bankroll("100000", "0", "100000");
		BettingOpportunity opportunity = binaryOpportunity("0.05");
		StrategyDecision match = engine.decide(
				config, opportunity, bankroll, new PortfolioExposure(money("1500"), BigDecimal.ZERO, BigDecimal.ZERO));
		assertThat(match.stake()).isEqualByComparingTo("1500");
		assertThat(match.reasons()).contains(StrategyDecisionReason.MATCH_EXPOSURE_CAPPED);
		StrategyDecision combined = engine.decide(
				config,
				opportunity,
				bankroll,
				new PortfolioExposure(money("1500"), money("3500"), money("9000")));
		assertThat(combined.stake()).isEqualByComparingTo("1000");
		assertThat(combined.reasons()).contains(
				StrategyDecisionReason.MATCH_EXPOSURE_CAPPED,
				StrategyDecisionReason.LEAGUE_EXPOSURE_CAPPED,
				StrategyDecisionReason.DAILY_EXPOSURE_CAPPED);
	}

	@Test
	void existingOverLimitExposureRejectsWithoutThrowing() {
		StrategyDecision decision = engine.decide(
				defensiveLike(),
				binaryOpportunity("0.05"),
				bankroll("100000", "0", "100000"),
				new PortfolioExposure(money("5000"), BigDecimal.ZERO, BigDecimal.ZERO));
		assertThat(decision.status()).isEqualTo(StrategyDecisionStatus.REJECTED);
		assertThat(decision.stake()).isEqualByComparingTo("0");
		assertThat(decision.reasons()).contains(StrategyDecisionReason.NO_EXPOSURE_CAPACITY);
		assertThat(decision.reasons()).contains(StrategyDecisionReason.MATCH_EXPOSURE_CAPPED);
	}

	@Test
	void vaultBalanceDoesNotChangeStake() {
		StrategyConfig config = defensiveLike();
		BettingOpportunity opportunity = binaryOpportunity("0.05");
		StrategyDecision withoutVault = engine.decide(
				config, opportunity, bankroll("100000", "0", "100000"), PortfolioExposure.none());
		StrategyDecision withVault = engine.decide(
				config, opportunity, bankroll("100000", "50000", "100000"), PortfolioExposure.none());
		assertThat(withVault.stake()).isEqualByComparingTo(withoutVault.stake());
		assertThat(withVault.status()).isEqualTo(withoutVault.status());
		assertThat(withVault.appliedStakeRate()).isEqualByComparingTo(withoutVault.appliedStakeRate());
		assertThat(withVault.reasons()).isEqualTo(withoutVault.reasons());
	}

	@Test
	void zeroActiveBankrollRejectsWithoutUsingVault() {
		BankrollState emptyActive = new BankrollState(
				OWNER,
				BigDecimal.ZERO,
				money("50000"),
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				money("50000"));
		StrategyDecision decision = engine.decide(
				defensiveLike(), binaryOpportunity("0.05"), emptyActive, PortfolioExposure.none());
		assertThat(decision.status()).isEqualTo(StrategyDecisionStatus.REJECTED);
		assertThat(decision.reasons()).containsExactly(StrategyDecisionReason.NO_ACTIVE_BANKROLL);
		assertThat(decision.stake()).isEqualByComparingTo("0");
	}

	@Test
	void flatStakeDoesNotUseKellyAndIgnoresNonPositiveExpectedReturn() {
		StrategyConfig config = new StrategyPresetFactory().configFor(StrategyPreset.FLAT_STAKE);
		BettingOpportunity poorEv = new BettingOpportunity(
				"opp-flat",
				"event-1",
				"league-1",
				DATE,
				new BigDecimal("2.00"),
				new BigDecimal("0.03"),
				SettlementProbabilityDistribution.binary(new BigDecimal("0.40")));
		StrategyDecision decision = engine.decide(config, poorEv, bankroll("100000", "0", "100000"), PortfolioExposure.none());
		assertThat(decision.status()).isEqualTo(StrategyDecisionStatus.ACCEPTED);
		assertThat(decision.stake()).isEqualByComparingTo("1000");
		assertThat(decision.fullKellyFraction()).isNull();
		assertThat(decision.expectedReturnRate()).isNegative();
	}

	@Test
	void fractionalKellyRejectsNonPositiveExpectedReturn() {
		StrategyDecision decision = engine.decide(
				defensiveLike(),
				new BettingOpportunity(
						"opp-neg",
						"event-1",
						"league-1",
						DATE,
						new BigDecimal("2.00"),
						new BigDecimal("0.05"),
						SettlementProbabilityDistribution.binary(new BigDecimal("0.40"))),
				bankroll("100000", "0", "100000"),
				PortfolioExposure.none());
		assertThat(decision.status()).isEqualTo(StrategyDecisionStatus.REJECTED);
		assertThat(decision.reasons()).containsExactly(StrategyDecisionReason.NON_POSITIVE_EXPECTED_RETURN);
		assertThat(decision.stake()).isEqualByComparingTo("0");
	}

	@Test
	void engineDoesNotMutateInputs() {
		BankrollState bankroll = bankroll("100000", "0", "100000");
		PortfolioExposure exposure = new PortfolioExposure(money("100"), money("200"), money("300"));
		BigDecimal active = bankroll.activeBankroll();
		engine.decide(defensiveLike(), binaryOpportunity("0.05"), bankroll, exposure);
		assertThat(bankroll.activeBankroll()).isEqualByComparingTo(active);
		assertThat(exposure.matchExposureAmount()).isEqualByComparingTo("100");
	}

	@Test
	void presetFactoryConfigIsUsableWithoutEngineKnowingThePreset() {
		StrategyDecision decision = engine.decide(
				presets.configFor(StrategyPreset.DEFENSIVE),
				binaryOpportunity("0.05"),
				bankroll("100000", "0", "100000"),
				PortfolioExposure.none());
		assertThat(decision.status()).isEqualTo(StrategyDecisionStatus.ACCEPTED);
		assertThat(decision.stake()).isPositive();
	}

	private void assertAcceptedWithout(StrategyConfig config, BankrollState bankroll, StrategyDecisionReason absent) {
		StrategyDecision decision = engine.decide(config, binaryOpportunity("0.05"), bankroll, PortfolioExposure.none());
		assertThat(decision.status()).isEqualTo(StrategyDecisionStatus.ACCEPTED);
		assertThat(decision.reasons()).doesNotContain(absent, StrategyDecisionReason.DRAWDOWN_REDUCTION_APPLIED);
	}

	private void assertHasReason(
			StrategyConfig config,
			BettingOpportunity opportunity,
			BankrollState bankroll,
			StrategyDecisionReason reason,
			boolean reduction) {
		StrategyDecision decision = engine.decide(config, opportunity, bankroll, PortfolioExposure.none());
		assertThat(decision.status()).isEqualTo(StrategyDecisionStatus.ACCEPTED);
		assertThat(decision.reasons()).contains(reason);
		if (reduction) {
			assertThat(decision.reasons()).contains(StrategyDecisionReason.DRAWDOWN_REDUCTION_APPLIED);
			assertThat(decision.reasons()).doesNotContain(StrategyDecisionReason.DRAWDOWN_WARNING);
		}
		else {
			assertThat(decision.reasons()).doesNotContain(StrategyDecisionReason.DRAWDOWN_REDUCTION_APPLIED);
		}
		assertThat(decision.stake()).isPositive();
	}

	private void assertPaused(StrategyConfig config, BettingOpportunity opportunity, BankrollState bankroll) {
		StrategyDecision decision = engine.decide(config, opportunity, bankroll, PortfolioExposure.none());
		assertThat(decision.status()).isEqualTo(StrategyDecisionStatus.PAUSED_DRAWDOWN);
		assertThat(decision.stake()).isEqualByComparingTo("0");
		assertThat(decision.reasons()).containsExactly(StrategyDecisionReason.DRAWDOWN_STOP);
	}

	private static StrategyConfig defensiveLike() {
		return new StrategyConfig(
				true,
				money("0.30"),
				StakingMode.FRACTIONAL_KELLY,
				money("0.25"),
				null,
				money("0.02"),
				money("0.03"),
				money("0.03"),
				money("0.05"),
				money("0.10"),
				money("0.10"),
				money("0.15"),
				money("0.50"),
				money("0.20"));
	}

	private static BettingOpportunity binaryOpportunity(String edge) {
		return new BettingOpportunity(
				"opp-1",
				"event-1",
				"league-1",
				DATE,
				new BigDecimal("2.00"),
				money(edge),
				SettlementProbabilityDistribution.binary(new BigDecimal("0.60")));
	}

	private static BankrollState bankroll(String active, String vault, String activeHwm) {
		BigDecimal activeAmount = money(active);
		BigDecimal vaultAmount = money(vault);
		BigDecimal hwm = money(activeHwm);
		return new BankrollState(
				OWNER,
				activeAmount,
				vaultAmount,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				hwm,
				hwm.max(activeAmount.add(vaultAmount)));
	}

	private static BigDecimal money(String value) {
		return new BigDecimal(value);
	}

}
