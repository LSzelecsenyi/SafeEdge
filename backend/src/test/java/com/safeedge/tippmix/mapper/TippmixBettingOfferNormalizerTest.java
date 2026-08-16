package com.safeedge.tippmix.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.BettingOffer;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.event.domain.MarketType;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.tippmix.TippmixOfferFixtures;
import com.safeedge.tippmix.dto.TippmixMarketDto;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class TippmixBettingOfferNormalizerTest {

	private final TippmixBettingOfferNormalizer normalizer = new TippmixBettingOfferNormalizer();

	@Test
	void asianHandicapMinusOne_normalizesHomeAndAwayLines() {
		BettingMarket market = singleMarket(TippmixOfferFixtures.asianHandicap("-1", "Djurgarden -1", "AIK Stockholm +1"));

		assertThat(market.marketType()).isEqualTo(MarketType.ASIAN_HANDICAP);
		assertThat(market.line()).isEqualByComparingTo("-1");
		assertThat(market.selections()).hasSize(2);
		assertSelection(market.selections().get(0), SelectionType.HOME, "-1");
		assertSelection(market.selections().get(1), SelectionType.AWAY, "1");
	}

	@Test
	void asianHandicapQuarterLine_preservesExactDecimals() {
		BettingMarket market =
				singleMarket(TippmixOfferFixtures.asianHandicap("-1.25", "Djurgarden -1,25", "AIK Stockholm +1,25"));

		assertThat(market.marketType()).isEqualTo(MarketType.ASIAN_HANDICAP);
		assertThat(market.line()).isEqualByComparingTo("-1.25");
		assertSelection(market.selections().get(0), SelectionType.HOME, "-1.25");
		assertSelection(market.selections().get(1), SelectionType.AWAY, "1.25");
	}

	@Test
	void europeanHandicap_mapsByOutcomeNoAndHungarianLabels() {
		BettingMarket market = singleMarket(TippmixOfferFixtures.europeanHandicap(
				TippmixOfferFixtures.outcome(3, "AIK Stockholm", "3.80"),
				TippmixOfferFixtures.outcome(1, "Djurgarden", "1.90"),
				TippmixOfferFixtures.outcome(2, "Döntetlen", "3.40")));

		assertThat(market.marketType()).isEqualTo(MarketType.EUROPEAN_HANDICAP);
		assertThat(market.line()).isEqualByComparingTo("-1");
		assertThat(market.selections())
				.extracting(BettingSelection::selectionType)
				.containsExactly(SelectionType.HOME, SelectionType.DRAW, SelectionType.AWAY);
		assertThat(market.selections())
				.extracting(BettingSelection::providerOutcomeName)
				.containsExactly("Djurgarden", "Döntetlen", "AIK Stockholm");
		assertThat(market.selections())
				.extracting(BettingSelection::externalOutcomeNo)
				.containsExactly(1, 2, 3);
		assertThat(market.selections()).allSatisfy(selection -> assertThat(selection.line()).isNull());
	}

	@Test
	void europeanHandicap_rejectsEnglishAliases() {
		assertThatThrownBy(() -> normalizer.normalize(TippmixOfferFixtures.prematchFootball(
						TippmixOfferFixtures.europeanHandicap(
								TippmixOfferFixtures.outcome(1, "home", "1.90"),
								TippmixOfferFixtures.outcome(2, "draw", "3.40"),
								TippmixOfferFixtures.outcome(3, "away", "3.80")))))
				.isInstanceOf(TippmixNormalizationException.class)
				.hasMessageContaining("home outcome name");
	}

	@Test
	void europeanHandicap_rejectsSwappedParticipants() {
		assertThatThrownBy(() -> normalizer.normalize(TippmixOfferFixtures.prematchFootball(
						TippmixOfferFixtures.europeanHandicap(
								TippmixOfferFixtures.outcome(1, "AIK Stockholm", "1.90"),
								TippmixOfferFixtures.outcome(2, "Döntetlen", "3.40"),
								TippmixOfferFixtures.outcome(3, "Djurgarden", "3.80")))))
				.isInstanceOf(TippmixNormalizationException.class)
				.hasMessageContaining("home outcome name");
	}

	@Test
	void europeanHandicap_rejectsMissingOutcomeNo() {
		assertThatThrownBy(() -> normalizer.normalize(TippmixOfferFixtures.prematchFootball(
						TippmixOfferFixtures.europeanHandicap(
								TippmixOfferFixtures.outcome(1, "Djurgarden", "1.90"),
								TippmixOfferFixtures.outcome(2, "Döntetlen", "3.40"),
								TippmixOfferFixtures.outcome(4, "AIK Stockholm", "3.80")))))
				.isInstanceOf(TippmixNormalizationException.class)
				.hasMessageContaining("outcomeNo=3");
	}

	@Test
	void doubleChance_mapsByOutcomeNoAndHungarianLabels() {
		BettingMarket market = singleMarket(TippmixOfferFixtures.doubleChance(
				TippmixOfferFixtures.outcome(2, "Djurgarden vagy AIK Stockholm", "1.30"),
				TippmixOfferFixtures.outcome(3, "Döntetlen vagy AIK Stockholm", "1.45"),
				TippmixOfferFixtures.outcome(1, "Djurgarden vagy Döntetlen", "1.25")));

		assertThat(market.marketType()).isEqualTo(MarketType.DOUBLE_CHANCE);
		assertThat(market.line()).isNull();
		assertThat(market.selections())
				.extracting(BettingSelection::selectionType)
				.containsExactly(SelectionType.HOME_OR_DRAW, SelectionType.HOME_OR_AWAY, SelectionType.DRAW_OR_AWAY);
		assertThat(market.selections())
				.extracting(BettingSelection::providerOutcomeName)
				.containsExactly(
						"Djurgarden vagy Döntetlen",
						"Djurgarden vagy AIK Stockholm",
						"Döntetlen vagy AIK Stockholm");
		assertThat(market.selections())
				.extracting(BettingSelection::externalOutcomeNo)
				.containsExactly(1, 2, 3);
	}

	@Test
	void doubleChance_rejectsEnglishAliases() {
		assertThatThrownBy(() -> normalizer.normalize(TippmixOfferFixtures.prematchFootball(
						TippmixOfferFixtures.doubleChance(
								TippmixOfferFixtures.outcome(1, "home or draw", "1.25"),
								TippmixOfferFixtures.outcome(2, "home or away", "1.30"),
								TippmixOfferFixtures.outcome(3, "draw or away", "1.45")))))
				.isInstanceOf(TippmixNormalizationException.class)
				.hasMessageContaining("home or draw outcome name");
	}

	@Test
	void doubleChance_rejectsWrongParticipantMapping() {
		assertThatThrownBy(() -> normalizer.normalize(TippmixOfferFixtures.prematchFootball(
						TippmixOfferFixtures.doubleChance(
								TippmixOfferFixtures.outcome(1, "AIK Stockholm vagy Döntetlen", "1.25"),
								TippmixOfferFixtures.outcome(2, "AIK Stockholm vagy Djurgarden", "1.30"),
								TippmixOfferFixtures.outcome(3, "Döntetlen vagy Djurgarden", "1.45")))))
				.isInstanceOf(TippmixNormalizationException.class)
				.hasMessageContaining("home or draw outcome name");
	}

	@Test
	void exactScore_isIgnored() {
		BettingOffer offer = normalizer.normalize(TippmixOfferFixtures.prematchFootball(TippmixOfferFixtures.exactScore()));

		assertThat(offer.markets()).isEmpty();
	}

	@Test
	void asianHandicapWithThreeOutcomes_fails() {
		TippmixMarketDto malformed = new TippmixMarketDto(
				8001L,
				"Ázsiai Hendikep -1",
				10,
				1,
				2,
				96,
				2,
				3,
				false,
				3,
				"-1",
				List.of(5, 6017),
				List.of(
						TippmixOfferFixtures.outcome(1, "Djurgarden -1", "1.57"),
						TippmixOfferFixtures.outcome(2, "draw", "3.00"),
						TippmixOfferFixtures.outcome(3, "AIK Stockholm +1", "2.12")));

		assertThatThrownBy(() -> normalizer.normalize(TippmixOfferFixtures.prematchFootball(malformed)))
				.isInstanceOf(TippmixNormalizationException.class)
				.hasMessageContaining("exactly 2 outcomes");
	}

	@Test
	void asianHandicapWithInvalidLine_fails() {
		TippmixMarketDto malformed = new TippmixMarketDto(
				8001L,
				"Ázsiai Hendikep",
				10,
				1,
				2,
				96,
				2,
				3,
				false,
				2,
				"not-a-number",
				List.of(5, 6017),
				List.of(
						TippmixOfferFixtures.outcome(1, "Djurgarden -1", "1.57"),
						TippmixOfferFixtures.outcome(2, "AIK Stockholm +1", "2.12")));

		assertThatThrownBy(() -> normalizer.normalize(TippmixOfferFixtures.prematchFootball(malformed)))
				.isInstanceOf(TippmixNormalizationException.class)
				.hasMessageContaining("specialOddsValue");
	}

	@Test
	void asianHandicapWithSwappedParticipants_fails() {
		assertThatThrownBy(() -> normalizer.normalize(TippmixOfferFixtures.prematchFootball(
						TippmixOfferFixtures.asianHandicap("-1", "AIK Stockholm +1", "Djurgarden -1"))))
				.isInstanceOf(TippmixNormalizationException.class)
				.hasMessageContaining("home outcome name");
	}

	@Test
	void startTime_isConvertedToUtcInstant() {
		BettingOffer offer = normalizer.normalize(TippmixOfferFixtures.prematchFootball());

		assertThat(offer.event().startTime()).isEqualTo(TippmixOfferFixtures.KICKOFF.toInstant());
	}

	private BettingMarket singleMarket(TippmixMarketDto market) {
		BettingOffer offer = normalizer.normalize(TippmixOfferFixtures.prematchFootball(market));
		assertThat(offer.markets()).hasSize(1);
		return offer.markets().getFirst();
	}

	private static void assertSelection(BettingSelection selection, SelectionType type, String line) {
		assertThat(selection.selectionType()).isEqualTo(type);
		assertThat(selection.line()).isEqualByComparingTo(new BigDecimal(line));
	}

}
