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
		// TODO: interval, type, and market are verified request values only;
		// their exact Tippmix semantics remain unknown. Do not invent mappings.
		return new TippmixResultRequest(1, 0, 0, 3, "date", 1, "");
	}

}
