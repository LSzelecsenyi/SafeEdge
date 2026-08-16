package com.safeedge.tippmix.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TippmixMarketDto(
		Long marketId,
		String marketName,
		Integer marketRealNo,
		Integer marketStatus,
		Integer marketType,
		Integer marketSubType,
		Integer marketTypePriority,
		Integer marketVersion,
		Boolean mainMarket,
		Integer outcomeCount,
		String specialOddsValue,
		List<Integer> marketGroupIds,
		List<TippmixOutcomeDto> outcomes) {
}
