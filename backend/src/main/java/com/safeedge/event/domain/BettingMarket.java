package com.safeedge.event.domain;

import java.math.BigDecimal;
import java.util.List;

public record BettingMarket(
		String provider,
		String externalMarketId,
		Integer providerMarketRealNo,
		String providerMarketName,
		Integer providerMarketType,
		Integer providerMarketSubType,
		Integer providerMarketVersion,
		MarketType marketType,
		BigDecimal line,
		List<BettingSelection> selections) {
}
