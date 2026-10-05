package com.payguard.automation.fixtures;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.payguard.automation.config.Config;
import java.net.URI;
import java.util.UUID;

public final class ProviderStubs {

    private ProviderStubs() {
    }

    public static void declineAuthorize(UUID orderId) {
        client().register(post(urlEqualTo("/v1/authorize"))
                .withRequestBody(matchingJsonPath("$.orderId", equalTo(orderId.toString())))
                .atPriority(1)
                .willReturn(json(400, "{\"error\":\"DECLINED\"}")));
    }

    public static void failCapture(UUID orderId) {
        client().register(post(urlEqualTo("/v1/capture"))
                .withRequestBody(matchingJsonPath("$.orderId", equalTo(orderId.toString())))
                .atPriority(1)
                .willReturn(json(500, "{\"error\":\"PROVIDER_UNAVAILABLE\"}")));
    }

    public static void failRefund(UUID orderId) {
        client().register(post(urlEqualTo("/v1/refunds"))
                .withRequestBody(matchingJsonPath("$.orderId", equalTo(orderId.toString())))
                .atPriority(1)
                .willReturn(json(500, "{\"error\":\"PROVIDER_UNAVAILABLE\"}")));
    }

    public static void delayAuthorize(UUID orderId) {
        client().register(post(urlEqualTo("/v1/authorize"))
                .withRequestBody(matchingJsonPath("$.orderId", equalTo(orderId.toString())))
                .atPriority(1)
                .willReturn(json(200, "{\"transactionId\":\"txn-late\",\"status\":\"AUTHORIZED\"}")
                        .withFixedDelay(5_000)));
    }

    private static WireMock client() {
        URI uri = URI.create(Config.wiremockUrl());
        return new WireMock(uri.getHost(), uri.getPort());
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(int status, String body) {
        return aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }
}
