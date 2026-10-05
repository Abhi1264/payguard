package com.payguard.automation.clients;

import com.payguard.automation.config.Config;
import com.payguard.automation.models.CreateOrderRequest;

public final class OrderApiClient {

    public ApiResult create(CreateOrderRequest request) {
        return ApiResult.of(ApiClient.post(Config.orderServiceUrl(), "/orders", request));
    }

    public ApiResult get(Object id) {
        return ApiResult.of(ApiClient.get(Config.orderServiceUrl(), "/orders/{id}", id));
    }
}
