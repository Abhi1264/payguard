package com.payguard.order;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(@Qualifier("userRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public void requireExists(UUID userId) {
        try {
            restClient.get()
                    .uri("/users/{id}", userId)
                    .header(Correlation.HEADER, Correlation.current())
                    .exchange((request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new ApiException(404, "NOT_FOUND", "User not found");
                        }
                        if (response.getStatusCode().isError()) {
                            throw new ApiException(502, "INTERNAL_ERROR", "User service error");
                        }
                        return null;
                    });
        } catch (ApiException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ApiException(502, "INTERNAL_ERROR", "User service unavailable");
        }
    }
}
