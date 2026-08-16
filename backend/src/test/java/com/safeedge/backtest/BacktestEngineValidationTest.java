package com.safeedge.backtest;

import static com.safeedge.backtest.BacktestFixtures.OWNER;
import static com.safeedge.backtest.BacktestFixtures.STARTING;
import static com.safeedge.backtest.BacktestFixtures.T10;
import static com.safeedge.backtest.BacktestFixtures.T11;
import static com.safeedge.backtest.BacktestFixtures.T12;
import static com.safeedge.backtest.BacktestFixtures.T18;
import static com.safeedge.backtest.BacktestFixtures.flatTwoPercent;
import static com.safeedge.backtest.BacktestFixtures.homeZero;
import static com.safeedge.backtest.BacktestFixtures.request;
import static com.safeedge.backtest.BacktestFixtures.win;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.strategy.BettingOpportunity;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class BacktestEngineValidationTest {

	@Test
	void missingEventResultIsRejected() {
		assertThatThrownBy(() -> request(
				flatTwoPercent(),
				List.of(homeZero("a", "e1", "l1", T10, "2.00", "0.05")),
				List.of(win("other", T18))))
				.isInstanceOf(BacktestException.class)
				.hasMessageContaining("no event result");
	}

	@Test
	void duplicateEventResultIsRejected() {
		assertThatThrownBy(() -> request(
				flatTwoPercent(),
				List.of(homeZero("a", "e1", "l1", T10, "2.00", "0.05")),
				List.of(win("e1", T18), win("e1", T12))))
				.isInstanceOf(BacktestException.class)
				.hasMessageContaining("Duplicate historical event result");
	}

	@Test
	void outOfOrderOpportunitiesAreRejectedWithoutSilentSort() {
		assertThatThrownBy(() -> request(
				flatTwoPercent(),
				List.of(homeZero("b", "e2", "l1", T12, "2.00", "0.05"), homeZero("a", "e1", "l1", T10, "2.00", "0.05")),
				List.of(win("e1", T18), win("e2", T18))))
				.isInstanceOf(BacktestException.class)
				.hasMessageContaining("non-decreasing decisionAt");
	}

	@Test
	void decisionAtOnOrAfterSettlementAtIsRejected() {
		assertThatThrownBy(() -> request(
				flatTwoPercent(),
				List.of(homeZero("a", "e1", "l1", T12, "2.00", "0.05")),
				List.of(win("e1", T12))))
				.isInstanceOf(BacktestException.class)
				.hasMessageContaining("look-ahead");
	}

	@Test
	void duplicateOpportunityIdIsRejected() {
		assertThatThrownBy(() -> request(
				flatTwoPercent(),
				List.of(homeZero("same", "e1", "l1", T10, "2.00", "0.05"), homeZero("same", "e2", "l1", T11, "2.00", "0.05")),
				List.of(win("e1", T18), win("e2", T18))))
				.isInstanceOf(BacktestException.class)
				.hasMessageContaining("Duplicate opportunityId");
	}

	@Test
	void nonPositiveStartingBankrollIsRejected() {
		assertThatThrownBy(() -> new BacktestRequest(
				OWNER,
				BigDecimal.ZERO,
				flatTwoPercent(),
				List.of(),
				List.of(),
				null))
				.isInstanceOf(BacktestException.class)
				.hasMessageContaining("startingBankroll");
	}

	@Test
	void invalidMaxAcceptedBetsIsRejected() {
		assertThatThrownBy(() -> request(
				flatTwoPercent(),
				List.of(homeZero("a", "e1", "l1", T10, "2.00", "0.05")),
				List.of(win("e1", T18)),
				0))
				.isInstanceOf(BacktestException.class)
				.hasMessageContaining("maxAcceptedBets");
	}

	@Test
	void nullRequiredRequestFieldsAreRejected() {
		assertThatThrownBy(() -> new BacktestRequest(null, STARTING, flatTwoPercent(), List.of(), List.of(), null))
				.isInstanceOf(BacktestException.class)
				.hasMessageContaining("ownerId");
		assertThatThrownBy(() -> new BacktestRequest(OWNER, STARTING, null, List.of(), List.of(), null))
				.isInstanceOf(BacktestException.class)
				.hasMessageContaining("strategyConfig");
		assertThatThrownBy(() -> new BacktestRequest(OWNER, STARTING, flatTwoPercent(), null, List.of(), null))
				.isInstanceOf(BacktestException.class)
				.hasMessageContaining("opportunities");
		assertThatThrownBy(() -> new BacktestRequest(OWNER, STARTING, flatTwoPercent(), List.of(), null, null))
				.isInstanceOf(BacktestException.class)
				.hasMessageContaining("eventResults");
	}

	@Test
	void mismatchedOpportunityAndSelectionOddsAreRejected() {
		HistoricalBettingOpportunity valid = homeZero("a", "e1", "l1", T10, "2.00", "0.05");
		BettingMarket market = valid.market();
		BettingSelection mismatched = new BettingSelection(
				market.selections().getFirst().provider(),
				market.selections().getFirst().externalOutcomeNo(),
				market.selections().getFirst().externalOutcomeRealNo(),
				market.selections().getFirst().providerOutcomeName(),
				market.selections().getFirst().selectionType(),
				market.selections().getFirst().line(),
				new BigDecimal("1.90"));
		BettingMarket marketWithMismatch = new BettingMarket(
				market.provider(),
				market.externalMarketId(),
				market.providerMarketRealNo(),
				market.providerMarketName(),
				market.providerMarketType(),
				market.providerMarketSubType(),
				market.providerMarketVersion(),
				market.marketType(),
				market.line(),
				List.of(mismatched, market.selections().get(1)));
		BettingOpportunity opportunity = valid.opportunity();
		assertThatThrownBy(() -> new HistoricalBettingOpportunity(opportunity, marketWithMismatch, mismatched, T10))
				.isInstanceOf(BacktestException.class)
				.hasMessageContaining("opportunity odds must match selection observation odds");
	}

	@Test
	void engineRejectsNullRequest() {
		assertThatThrownBy(() -> new BacktestEngine().run(null))
				.isInstanceOf(BacktestException.class)
				.hasMessageContaining("Backtest request is required");
	}

}
