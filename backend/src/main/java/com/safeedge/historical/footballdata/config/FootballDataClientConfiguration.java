package com.safeedge.historical.footballdata.config;

import com.safeedge.historical.footballdata.client.FootballDataClient;
import com.safeedge.historical.footballdata.client.RestClientFootballDataClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(FootballDataProperties.class)
class FootballDataClientConfiguration {

	@Bean
	FootballDataClient footballDataClient(FootballDataProperties properties) {
		return new RestClientFootballDataClient(RestClient.builder().baseUrl(properties.baseUrl()).build());
	}

}
