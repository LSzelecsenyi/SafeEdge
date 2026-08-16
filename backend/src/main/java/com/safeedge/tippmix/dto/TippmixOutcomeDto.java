package com.safeedge.tippmix.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TippmixOutcomeDto(
		Integer outcomeNo,
		String outcomeName,
		Integer outcomeRealNo,
		BigDecimal fixedOdds,
		Integer outcomeResult,
		Boolean isCustomBet) {
}
