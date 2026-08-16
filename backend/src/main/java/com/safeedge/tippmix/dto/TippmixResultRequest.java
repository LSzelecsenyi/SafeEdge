package com.safeedge.tippmix.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TippmixResultRequest(
		Integer sportId,
		Integer competitionGroupId,
		Integer competitionId,
		Integer interval,
		String type,
		Integer market,
		String searchBy) {

	public static TippmixResultRequest verifiedFootballResults() {
		return new TippmixResultRequest(1, 0, 0, 3, "date", 1, "");
	}

}
