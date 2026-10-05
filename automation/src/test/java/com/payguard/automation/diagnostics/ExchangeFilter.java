package com.payguard.automation.diagnostics;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import java.util.Locale;
import java.util.Set;

public final class ExchangeFilter implements Filter {

    private static final Set<String> REDACTED = Set.of(
            "authorization", "cookie", "set-cookie", "proxy-authorization");

    @Override
    public Response filter(FilterableRequestSpecification request, FilterableResponseSpecification responseSpec,
            FilterContext context) {
        Response response = context.next(request, responseSpec);
        ExchangeLog.record(render(request, response));
        return response;
    }

    private static String render(FilterableRequestSpecification request, Response response) {
        StringBuilder text = new StringBuilder();
        text.append(request.getMethod()).append(' ').append(request.getURI()).append('\n');
        request.getHeaders().forEach(header -> {
            String name = header.getName();
            String value = REDACTED.contains(name.toLowerCase(Locale.ROOT)) ? "[redacted]" : header.getValue();
            text.append(name).append(": ").append(value).append('\n');
        });
        Object body = request.getBody();
        if (body != null) {
            text.append('\n').append(body).append('\n');
        }
        text.append("\nstatus: ").append(response.statusCode()).append('\n');
        String requestId = response.getHeader("X-Request-Id");
        if (requestId != null) {
            text.append("X-Request-Id: ").append(requestId).append('\n');
        }
        text.append(response.asString());
        return text.toString();
    }
}
