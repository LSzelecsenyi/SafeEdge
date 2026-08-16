package com.safeedge.tippmix.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "safeedge.providers.tippmix")
public record TippmixProperties(@DefaultValue("https://api.tippmix.hu") String baseUrl) {
}
