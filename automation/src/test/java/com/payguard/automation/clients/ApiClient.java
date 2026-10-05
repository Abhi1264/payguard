package com.payguard.automation.clients;

import com.payguard.automation.config.Config;
import com.payguard.automation.diagnostics.ExchangeFilter;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.net.ConnectException;
import java.util.UUID;
import java.util.function.Supplier;

public final class ApiClient {

    private static final RestAssuredConfig CONFIG = RestAssuredConfig.config()
            .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                    .defaultObjectMapperType(ObjectMapperType.JACKSON_3))
            .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.socket.timeout", 30_000)
                    .setParam("http.connection.timeout", 5_000));

    private ApiClient() {
    }

    public static Response post(String baseUrl, String path, Object body) {
        return send(() -> RestAssured.given().spec(spec(baseUrl)).body(body).post(path));
    }

    public static Response post(String baseUrl, String path, Object body, String headerName, String headerValue) {
        return send(() -> RestAssured.given().spec(spec(baseUrl)).header(headerName, headerValue).body(body).post(path));
    }

    public static Response postRaw(String baseUrl, String path, String rawBody) {
        return send(() -> RestAssured.given().spec(spec(baseUrl)).body(rawBody).post(path));
    }

    public static Response postEmpty(String baseUrl, String path, Object... pathParams) {
        return send(() -> RestAssured.given().spec(spec(baseUrl)).post(path, pathParams));
    }

    public static Response get(String baseUrl, String path, Object... pathParams) {
        return send(() -> RestAssured.given().spec(spec(baseUrl)).get(path, pathParams));
    }

    private static RequestSpecification spec(String baseUrl) {
        return new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("X-Request-Id", UUID.randomUUID().toString())
                .addHeader("Authorization", "Bearer " + Config.apiToken())
                .addFilter(new ExchangeFilter())
                .addFilter(new AllureRestAssured())
                .setConfig(CONFIG)
                .build();
    }

    private static Response send(Supplier<Response> request) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return request.get();
            } catch (RuntimeException ex) {
                if (attempt == 3 || !isConnectionFailure(ex)) {
                    throw ex;
                }
                last = ex;
                try {
                    Thread.sleep(200L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        throw last;
    }

    private static boolean isConnectionFailure(Throwable ex) {
        while (ex != null) {
            if (ex instanceof ConnectException || "ConnectTimeoutException".equals(ex.getClass().getSimpleName())) {
                return true;
            }
            ex = ex.getCause();
        }
        return false;
    }
}
