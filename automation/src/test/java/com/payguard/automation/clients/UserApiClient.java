package com.payguard.automation.clients;

import com.payguard.automation.config.Config;
import com.payguard.automation.models.CreateUserRequest;

public final class UserApiClient {

    public ApiResult create(CreateUserRequest request) {
        return ApiResult.of(ApiClient.post(Config.userServiceUrl(), "/users", request));
    }

    public ApiResult get(Object id) {
        return ApiResult.of(ApiClient.get(Config.userServiceUrl(), "/users/{id}", id));
    }
}
