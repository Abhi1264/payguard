package com.payguard.automation.support;

import com.payguard.automation.config.Config;
import com.payguard.automation.diagnostics.Diagnostics;
import com.payguard.automation.diagnostics.ExchangeLog;
import io.qameta.allure.Allure;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public final class PayGuardExtension implements BeforeAllCallback, AfterTestExecutionCallback {

    private static final Object LOCK = new Object();
    private static volatile boolean ready;
    private static ComposeContainer compose;

    @Override
    public void beforeAll(ExtensionContext context) {
        ensureStarted();
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (context.getExecutionException().isPresent()) {
            attachFailures();
        } else {
            ExchangeLog.clear();
            Diagnostics.clear();
        }
    }

    public static void ensureStarted() {
        if (ready) {
            return;
        }
        synchronized (LOCK) {
            if (ready) {
                return;
            }
            if (!stackHealthy()) {
                startCompose();
            }
            writeEnvironment();
            ready = true;
        }
    }

    public static void attachFailures() {
        String http = ExchangeLog.renderAndClear();
        if (http != null) {
            Allure.attachment("HTTP exchange", http);
        }
        String database = Diagnostics.renderAndClear();
        if (database != null) {
            Allure.attachment("Database state", database);
        }
    }

    private static boolean stackHealthy() {
        boolean user = healthy(Config.userServiceUrl() + "/actuator/health");
        boolean order = healthy(Config.orderServiceUrl() + "/actuator/health");
        boolean payment = healthy(Config.paymentServiceUrl() + "/actuator/health");
        if (user && order && payment) {
            if (!healthy(Config.wiremockUrl() + "/__admin/mappings")) {
                throw new IllegalStateException("PayGuard services are up but WireMock is not. Run make down, then make up.");
            }
            return true;
        }
        if (user || order || payment) {
            throw new IllegalStateException("PayGuard is only partly up. Run make down, then make up.");
        }
        return false;
    }

    private static boolean healthy(String url) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
            HttpResponse<Void> response = client.send(
                    HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2)).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception ex) {
            return false;
        }
    }

    @SuppressWarnings("resource")
    private static void startCompose() {
        ComposeContainer container = new ComposeContainer(composeFile().toFile());
        container = container.withBuild(true)
                .withRemoveVolumes(true)
                .withStartupTimeout(Duration.ofMinutes(8))
                .withExposedService("user-service", 8081, Wait.forHttp("/actuator/health").forStatusCode(200))
                .withExposedService("order-service", 8082, Wait.forHttp("/actuator/health").forStatusCode(200))
                .withExposedService("payment-service", 8083, Wait.forHttp("/actuator/health").forStatusCode(200))
                .withExposedService("postgres", 5432, Wait.forListeningPort())
                .withExposedService("wiremock", 8080, Wait.forHttp("/__admin/mappings").forStatusCode(200));
        try {
            container.start();
        } catch (RuntimeException ex) {
            container.close();
            throw ex;
        }
        compose = container;
        Runtime.getRuntime().addShutdownHook(new Thread(container::close));
        System.setProperty("USER_SERVICE_URL", serviceUrl("user-service", 8081));
        System.setProperty("ORDER_SERVICE_URL", serviceUrl("order-service", 8082));
        System.setProperty("PAYMENT_SERVICE_URL", serviceUrl("payment-service", 8083));
        System.setProperty("WIREMOCK_URL", serviceUrl("wiremock", 8080));
        String dbHost = compose.getServiceHost("postgres", 5432);
        Integer dbPort = compose.getServicePort("postgres", 5432);
        System.setProperty("DB_URL", "jdbc:postgresql://" + dbHost + ":" + dbPort + "/payguard");
        System.setProperty("DB_USERNAME", "payguard");
        System.setProperty("DB_PASSWORD", "");
        System.setProperty("API_TOKEN", "local");
    }

    private static String serviceUrl(String service, int port) {
        return "http://" + compose.getServiceHost(service, port) + ":" + compose.getServicePort(service, port);
    }

    private static Path composeFile() {
        Path cwd = Path.of("").toAbsolutePath();
        Path direct = cwd.resolve("docker-compose.yml");
        if (Files.exists(direct)) {
            return direct;
        }
        Path parent = cwd.resolve("../docker-compose.yml").normalize();
        if (Files.exists(parent)) {
            return parent;
        }
        throw new IllegalStateException("docker-compose.yml not found from " + cwd);
    }

    private static void writeEnvironment() {
        String directory = System.getProperty("allure.results.directory", "target/allure-results");
        Path file = Path.of(directory, "environment.properties");
        String body = """
                user.service.url=%s
                order.service.url=%s
                payment.service.url=%s
                wiremock.url=%s
                """.formatted(
                Config.userServiceUrl(),
                Config.orderServiceUrl(),
                Config.paymentServiceUrl(),
                Config.wiremockUrl());
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, body);
        } catch (Exception ex) {
            throw new IllegalStateException("could not write Allure environment", ex);
        }
    }
}
