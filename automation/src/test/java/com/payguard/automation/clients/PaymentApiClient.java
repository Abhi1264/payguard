package com.payguard.automation.clients;

import com.payguard.automation.config.Config;
import com.payguard.automation.models.CreatePaymentRequest;

public final class PaymentApiClient {

    public ApiResult create(CreatePaymentRequest request) {
        return ApiResult.of(ApiClient.post(Config.paymentServiceUrl(), "/payments", request));
    }

    public ApiResult createThatFailsOrderUpdate(CreatePaymentRequest request) {
        return ApiResult.of(ApiClient.post(
                Config.paymentServiceUrl(), "/payments", request, "X-Fail-Order-Update", "true"));
    }

    public ApiResult postRaw(String body) {
        return ApiResult.of(ApiClient.postRaw(Config.paymentServiceUrl(), "/payments", body));
    }

    public ApiResult get(Object id) {
        return ApiResult.of(ApiClient.get(Config.paymentServiceUrl(), "/payments/{id}", id));
    }

    public ApiResult refund(Object id) {
        return ApiResult.of(ApiClient.postEmpty(Config.paymentServiceUrl(), "/payments/{id}/refund", id));
    }
}
