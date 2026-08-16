package com.safeedge.historical.footballdata.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.footballdata.mapper.FootballDataLeague;
import org.junit.jupiter.api.Test;

class FootballDataPathsTest {

	@Test
	void seasonAndLeagueBecomeSourcePathSegment() {
		FootballSeason season = new FootballSeason(2023, 2024);
		assertThat(FootballDataPaths.seasonCode(season)).isEqualTo("2324");
		assertThat(FootballDataPaths.csvPath(FootballDataLeague.E0, season)).isEqualTo("/mmz4281/2324/E0.csv");
		assertThat(FootballDataPaths.csvPath(FootballDataLeague.D1, new FootballSeason(2022, 2023)))
				.isEqualTo("/mmz4281/2223/D1.csv");
		assertThat(FootballDataPaths.csvPath(FootballDataLeague.SP1, season)).isEqualTo("/mmz4281/2324/SP1.csv");
		assertThat(FootballDataPaths.csvPath(FootballDataLeague.I1, season)).isEqualTo("/mmz4281/2324/I1.csv");
		assertThat(FootballDataPaths.csvPath(FootballDataLeague.F1, season)).isEqualTo("/mmz4281/2324/F1.csv");
	}

}
