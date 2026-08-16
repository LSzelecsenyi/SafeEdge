package com.safeedge.event.domain;

import java.util.List;

public record BettingOffer(BettingEvent event, List<BettingMarket> markets) {
}
