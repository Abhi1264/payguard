package com.payguard.order;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    @Bean(name = "userRestClient")
    RestClient userRestClient(ObjectProvider<RestClient.Builder> builders,
            @Value("${services.user-url}") String baseUrl,
            @Value("${api.token}") String token) {
        return builders.getObject().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
    }
}
