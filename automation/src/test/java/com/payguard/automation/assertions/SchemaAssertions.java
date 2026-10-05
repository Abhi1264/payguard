package com.payguard.automation.assertions;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

import io.restassured.response.Response;

public final class SchemaAssertions {

    private SchemaAssertions() {
    }

    public static void matches(Response response, String schema) {
        response.then().body(matchesJsonSchemaInClasspath(schema));
    }
}
