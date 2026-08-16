package com.safeedge.tippmix;

import com.safeedge.tippmix.dto.TippmixEventDto;
import com.safeedge.tippmix.dto.TippmixMarketDto;
import com.safeedge.tippmix.dto.TippmixOutcomeDto;
import com.safeedge.tippmix.dto.TippmixParticipantDto;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class TippmixOfferFixtures {

	public static final OffsetDateTime KICKOFF = OffsetDateTime.parse("2026-08-16T14:00:00+02:00");

	private TippmixOfferFixtures() {
	}

	public static TippmixEventDto prematchFootball(TippmixMarketDto... markets) {
		return event(1, 0, true, List.of(markets));
	}

	public static TippmixEventDto event(int sportId, int isLive, boolean hasVisiblePrematchMarket, List<TippmixMarketDto> markets) {
		return new TippmixEventDto(
				5311343L,
				72409632L,
				KICKOFF,
				"Djurgarden - AIK Stockholm",
				List.of(
						new TippmixParticipantDto(101L, "Djurgarden"),
						new TippmixParticipantDto(102L, "AIK Stockholm")),
				10L,
				11L,
				"Sweden",
				20L,
				21L,
				"Allsvenskan",
				sportId,
				"Football",
				isLive,
				0,
				false,
				hasVisiblePrematchMarket,
				0,
				87,
				87,
				1,
				1,
				5,
				markets,
				List.of());
	}

	public static TippmixMarketDto asianHandicap(String specialOddsValue, String homeOutcomeName, String awayOutcomeName) {
		return new TippmixMarketDto(
				8001L,
				"Ázsiai Hendikep " + specialOddsValue.replace("-", "-").replace(".", ","),
				10,
				1,
				2,
				96,
				2,
				3,
				false,
				2,
				specialOddsValue,
				List.of(5, 6017),
				List.of(
						outcome(1, homeOutcomeName, "1.57"),
						outcome(2, awayOutcomeName, "2.12")));
	}

	public static TippmixMarketDto europeanHandicap() {
		return europeanHandicap(
				outcome(1, "Djurgarden", "1.90"),
				outcome(2, "Döntetlen", "3.40"),
				outcome(3, "AIK Stockholm", "3.80"));
	}

	public static TippmixMarketDto europeanHandicap(TippmixOutcomeDto... outcomes) {
		return new TippmixMarketDto(
				8100L,
				"Hendikep 0:1",
				20,
				1,
				2,
				100,
				2,
				3,
				false,
				outcomes.length,
				"-1",
				List.of(5),
				List.of(outcomes));
	}

	public static TippmixMarketDto doubleChance() {
		return doubleChance(
				outcome(1, "Djurgarden vagy Döntetlen", "1.25"),
				outcome(2, "Djurgarden vagy AIK Stockholm", "1.30"),
				outcome(3, "Döntetlen vagy AIK Stockholm", "1.45"));
	}

	public static TippmixMarketDto doubleChance(TippmixOutcomeDto... outcomes) {
		return new TippmixMarketDto(
				8200L,
				"Kétesély",
				30,
				1,
				2,
				92,
				2,
				3,
				false,
				outcomes.length,
				null,
				List.of(),
				List.of(outcomes));
	}

	public static TippmixMarketDto exactScore() {
		return new TippmixMarketDto(
				8300L,
				"Pontos végeredmény",
				40,
				1,
				2,
				8,
				9,
				3,
				false,
				3,
				null,
				List.of(),
				List.of(
						outcome(1, "1:0", "8.00"),
						outcome(2, "2:0", "10.00"),
						outcome(3, "2:1", "9.00")));
	}

	public static TippmixOutcomeDto outcome(int no, String name, String odds) {
		return new TippmixOutcomeDto(no, name, no, new BigDecimal(odds), null, false);
	}

}
