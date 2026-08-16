package com.safeedge.tippmix.config;

import com.safeedge.tippmix.client.RestClientTippmixClient;
import com.safeedge.tippmix.client.RestClientTippmixResultClient;
import com.safeedge.tippmix.client.TippmixClient;
import com.safeedge.tippmix.client.TippmixResultClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TippmixProperties.class)
class TippmixClientConfiguration {

	@Bean
	TippmixClient tippmixClient(TippmixProperties properties) {
		return new RestClientTippmixClient(tippmixRestClient(properties));
	}

	@Bean
	TippmixResultClient tippmixResultClient(TippmixProperties properties) {
		return new RestClientTippmixResultClient(tippmixRestClient(properties));
	}

	private static RestClient tippmixRestClient(TippmixProperties properties) {
		return RestClient.builder().baseUrl(properties.baseUrl()).build();
	}

}
