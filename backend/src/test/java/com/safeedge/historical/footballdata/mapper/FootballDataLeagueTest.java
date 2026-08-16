package com.safeedge.historical.footballdata.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.historical.domain.CanonicalCompetition;
import org.junit.jupiter.api.Test;

class FootballDataLeagueTest {

	@Test
	void mapsBigFiveCanonicalCompetitions() {
		assertThat(FootballDataLeague.fromCanonical(CanonicalCompetition.PREMIER_LEAGUE)).isEqualTo(FootballDataLeague.E0);
		assertThat(FootballDataLeague.fromCanonical(CanonicalCompetition.BUNDESLIGA)).isEqualTo(FootballDataLeague.D1);
		assertThat(FootballDataLeague.fromCanonical(CanonicalCompetition.LA_LIGA)).isEqualTo(FootballDataLeague.SP1);
		assertThat(FootballDataLeague.fromCanonical(CanonicalCompetition.SERIE_A)).isEqualTo(FootballDataLeague.I1);
		assertThat(FootballDataLeague.fromCanonical(CanonicalCompetition.LIGUE_1)).isEqualTo(FootballDataLeague.F1);
		assertThat(FootballDataLeague.E0.canonicalCompetition()).isEqualTo(CanonicalCompetition.PREMIER_LEAGUE);
		assertThat(FootballDataLeague.fromCode("SP1")).isEqualTo(FootballDataLeague.SP1);
	}

}
