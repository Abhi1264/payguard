package com.payguard.payment;

import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    @Bean(name = "userRestClient")
    RestClient userRestClient(ObjectProvider<RestClient.Builder> builders,
            @Value("${services.user-url}") String baseUrl,
            @Value("${api.token}") String token) {
        return builders.getObject().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
    }

    @Bean(name = "orderRestClient")
    RestClient orderRestClient(ObjectProvider<RestClient.Builder> builders,
            @Value("${services.order-url}") String baseUrl,
            @Value("${api.token}") String token) {
        return builders.getObject().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
    }

    @Bean(name = "providerRestClient")
    RestClient providerRestClient(
            ObjectProvider<RestClient.Builder> builders,
            @Value("${services.provider-url}") String baseUrl,
            @Value("${services.provider-timeout}") Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return builders.getObject().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
