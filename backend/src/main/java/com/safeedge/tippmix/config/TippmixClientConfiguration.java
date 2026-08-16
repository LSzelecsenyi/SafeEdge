package com.safeedge.tippmix.config;

import com.safeedge.tippmix.client.RestClientTippmixClient;
import com.safeedge.tippmix.client.TippmixClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TippmixProperties.class)
class TippmixClientConfiguration {

	@Bean
	TippmixClient tippmixClient(TippmixProperties properties) {
		RestClient restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
		return new RestClientTippmixClient(restClient);
	}

}
