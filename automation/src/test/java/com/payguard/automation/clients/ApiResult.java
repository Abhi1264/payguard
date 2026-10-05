package com.payguard.automation.clients;

import io.restassured.response.Response;

public final class ApiResult {

    private final Response response;

    private ApiResult(Response response) {
        this.response = response;
    }

    public static ApiResult of(Response response) {
        return new ApiResult(response);
    }

    public int status() {
        return response.statusCode();
    }

    public String requestId() {
        return response.getHeader("X-Request-Id");
    }

    public String body() {
        return response.getBody() == null ? "" : response.asString();
    }

    public <T> T as(Class<T> type) {
        return response.as(type);
    }

    public Response response() {
        return response;
    }
}
