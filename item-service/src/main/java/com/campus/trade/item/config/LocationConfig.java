package com.campus.trade.item.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class LocationConfig {

    @Bean
    public RestClient restClient(LocationProperties locationProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = Math.toIntExact(locationProperties.getRequestTimeout().toMillis());
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        return RestClient.builder()
                .baseUrl(locationProperties.getAmap().getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
