package com.payguard.payment;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class OrderClient {

    private final RestClient restClient;

    public OrderClient(@Qualifier("orderRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public OrderView get(UUID orderId) {
        try {
            return restClient.get()
                    .uri("/orders/{id}", orderId)
                    .header(Correlation.HEADER, Correlation.current())
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ApiException(404, "NOT_FOUND", "Order not found");
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new ApiException(502, "INTERNAL_ERROR", "Order service error");
                    })
                    .body(OrderView.class);
        } catch (ApiException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ApiException(502, "INTERNAL_ERROR", "Order service unavailable");
        }
    }

    public void markPaid(UUID orderId) {
        post("/orders/{id}/pay", orderId, "Could not mark order paid");
    }

    public void markRefunded(UUID orderId) {
        post("/orders/{id}/refund", orderId, "Could not mark order refunded");
    }

    private void post(String path, UUID orderId, String failureMessage) {
        ApiException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                restClient.post()
                        .uri(path, orderId)
                        .header(Correlation.HEADER, Correlation.current())
                        .headers(headers -> {
                            String fault = faultHeader();
                            if (fault != null) {
                                headers.set("X-Fail-Order-Update", fault);
                            }
                        })
                        .exchange((request, response) -> {
                            if (response.getStatusCode().isError()) {
                                throw new ApiException(502, "INTERNAL_ERROR", failureMessage);
                            }
                            return null;
                        });
                return;
            } catch (ApiException ex) {
                last = ex;
            } catch (RestClientException ex) {
                last = new ApiException(502, "INTERNAL_ERROR", failureMessage);
            }
        }
        throw last;
    }

    private static String faultHeader() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader("X-Fail-Order-Update");
    }
}
