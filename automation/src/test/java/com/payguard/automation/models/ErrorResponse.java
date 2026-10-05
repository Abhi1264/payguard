package com.payguard.automation.models;

public record ErrorResponse(String timestamp, int status, String error, String message, String path) {
}
