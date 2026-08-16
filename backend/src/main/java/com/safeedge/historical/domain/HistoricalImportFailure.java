package com.safeedge.historical.domain;

public record HistoricalImportFailure(
		CanonicalCompetition competition,
		FootballSeason season,
		HistoricalImportFailureStage stage,
		String message) {

	public HistoricalImportFailure {
		if (competition == null) {
			throw new HistoricalDataException("competition is required");
		}
		if (season == null) {
			throw new HistoricalDataException("season is required");
		}
		if (stage == null) {
			throw new HistoricalDataException("stage is required");
		}
		if (message == null || message.isBlank()) {
			throw new HistoricalDataException("message is required");
		}
	}
}
