package com.safeedge.tippmix.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "safeedge.providers.tippmix")
public record TippmixProperties(
		@DefaultValue("https://api.tippmix.hu") String baseUrl,
		String manualEventId,
		@DefaultValue Collector collector,
		@DefaultValue Results results) {

	public record Collector(
			@DefaultValue("false") boolean enabled,
			@DefaultValue("PT5M") Duration fixedDelay,
			@DefaultValue("20") int pageSize,
			@DefaultValue("50") int maxPages) {
	}

	public record Results(
			@DefaultValue("false") boolean enabled,
			@DefaultValue("PT15M") Duration fixedDelay) {
	}
}
