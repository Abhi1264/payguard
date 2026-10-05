package com.payguard.automation.config;

public final class Config {

    private Config() {
    }

    public static String userServiceUrl() {
        return value("USER_SERVICE_URL", "http://localhost:8081");
    }

    public static String orderServiceUrl() {
        return value("ORDER_SERVICE_URL", "http://localhost:8082");
    }

    public static String paymentServiceUrl() {
        return value("PAYMENT_SERVICE_URL", "http://localhost:8083");
    }

    public static String wiremockUrl() {
        return value("WIREMOCK_URL", "http://localhost:8089");
    }

    public static String dbUrl() {
        return value("DB_URL", "jdbc:postgresql://localhost:5432/payguard");
    }

    public static String dbUsername() {
        return value("DB_USERNAME", "payguard");
    }

    public static String dbPassword() {
        String property = System.getProperty("DB_PASSWORD");
        if (property != null) {
            return property;
        }
        String env = System.getenv("DB_PASSWORD");
        return env == null ? "" : env;
    }

    public static String apiToken() {
        return value("API_TOKEN", "local");
    }

    private static String value(String key, String fallback) {
        String property = System.getProperty(key);
        if (property != null && !property.isBlank()) {
            return property;
        }
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return fallback;
    }
}
