package com.safeedge.historical.footballdata.mapper;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalDataException;

public enum FootballDataLeague {
	E0(CanonicalCompetition.PREMIER_LEAGUE),
	D1(CanonicalCompetition.BUNDESLIGA),
	SP1(CanonicalCompetition.LA_LIGA),
	I1(CanonicalCompetition.SERIE_A),
	F1(CanonicalCompetition.LIGUE_1);

	private final CanonicalCompetition canonicalCompetition;

	FootballDataLeague(CanonicalCompetition canonicalCompetition) {
		this.canonicalCompetition = canonicalCompetition;
	}

	public String code() {
		return name();
	}

	public CanonicalCompetition canonicalCompetition() {
		return canonicalCompetition;
	}

	public static FootballDataLeague fromCanonical(CanonicalCompetition competition) {
		if (competition == null) {
			throw new HistoricalDataException("canonical competition is required");
		}
		for (FootballDataLeague league : values()) {
			if (league.canonicalCompetition == competition) {
				return league;
			}
		}
		throw new HistoricalDataException("No football-data.co.uk league for " + competition);
	}

	public static FootballDataLeague fromCode(String code) {
		if (code == null || code.isBlank()) {
			throw new HistoricalDataException("football-data league code is required");
		}
		try {
			return FootballDataLeague.valueOf(code.trim());
		}
		catch (IllegalArgumentException ex) {
			throw new HistoricalDataException("Unsupported football-data.co.uk league code: " + code);
		}
	}

}
