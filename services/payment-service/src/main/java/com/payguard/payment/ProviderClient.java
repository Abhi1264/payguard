package com.payguard.payment;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ProviderClient {

    private final RestClient restClient;

    public ProviderClient(@Qualifier("providerRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public AuthorizeResponse authorize(Payment payment) {
        AuthorizeResponse response = post("/v1/authorize", body(payment), AuthorizeResponse.class);
        if (response == null || response.transactionId() == null || response.transactionId().isBlank()) {
            throw new ProviderException("Provider did not return a transaction id");
        }
        return response;
    }

    public void capture(Payment payment) {
        post("/v1/capture", body(payment), CaptureResponse.class);
    }

    public void refund(Payment payment) {
        post("/v1/refunds", body(payment), RefundResponse.class);
    }

    private ProviderBody body(Payment payment) {
        return new ProviderBody(
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProviderTransactionId());
    }

    private <T> T post(String path, ProviderBody body, Class<T> type) {
        try {
            return restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(Correlation.HEADER, Correlation.current())
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new ProviderException("Provider responded with " + response.getStatusCode().value());
                    })
                    .body(type);
        } catch (ProviderException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ProviderException(describe(ex), ex);
        }
    }

    private static String describe(Throwable ex) {
        StringBuilder detail = new StringBuilder();
        Throwable current = ex;
        while (current != null) {
            if (!detail.isEmpty()) {
                detail.append(" | ");
            }
            detail.append(current.getClass().getSimpleName());
            if (current.getMessage() != null) {
                detail.append(": ").append(current.getMessage());
            }
            current = current.getCause();
        }
        return detail.toString();
    }

    public record ProviderBody(UUID orderId, BigDecimal amount, String currency, String transactionId) {
    }

    public record AuthorizeResponse(String transactionId, String status) {
    }

    public record CaptureResponse(String transactionId, String status) {
    }

    public record RefundResponse(String refundId, String status) {
    }
}
