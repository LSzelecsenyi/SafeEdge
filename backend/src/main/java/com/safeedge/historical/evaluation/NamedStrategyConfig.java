package com.safeedge.historical.evaluation;

import com.safeedge.strategy.StrategyConfig;

public record NamedStrategyConfig(String name, StrategyConfig config) {

	public NamedStrategyConfig {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("strategy name is required");
		}
		if (config == null) {
			throw new IllegalArgumentException("strategy config is required");
		}
	}
}
