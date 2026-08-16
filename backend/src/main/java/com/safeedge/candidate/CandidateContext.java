package com.safeedge.candidate;

import java.time.LocalDate;

/**
 * Point-in-time identity for a candidate evaluation. The engine does not
 * generate ids and does not include a final result.
 */
public record CandidateContext(
		String opportunityId,
		String eventId,
		String leagueId,
		LocalDate bettingDate) {

	public CandidateContext {
		opportunityId = requireText(opportunityId, "opportunityId");
		eventId = requireText(eventId, "eventId");
		leagueId = requireText(leagueId, "leagueId");
		if (bettingDate == null) {
			throw new CandidateException("bettingDate is required");
		}
	}

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new CandidateException(name + " is required");
		}
		return value;
	}

}
