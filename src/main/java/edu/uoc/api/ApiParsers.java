package edu.uoc.api;

import io.javalin.http.Context;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

public final class ApiParsers {
    private ApiParsers() {}

    public static OffsetDateTime readRequiredOffsetDateTimeQuery(String raw, String name) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Missing required query param: " + name);
        }
        return parseOffsetDateTimeOrThrow(raw, name);
    }

    public static OffsetDateTime readOptionalOffsetDateTimeQuery(String raw, String name) {
        if (raw == null || raw.isBlank()) return null;
        return parseOffsetDateTimeOrThrow(raw, name);
    }

    public static OffsetDateTime parseRequiredOffsetDateTimeField(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + fieldName);
        }
        try {
            return OffsetDateTime.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid '" + fieldName + "' datetime. Use ISO-8601, e.g. 2026-02-21T20:30:00+01:00"
            );
        }
    }

    public static OffsetDateTime parseOptionalOffsetDateTimeField(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) return null;
        return parseRequiredOffsetDateTimeField(raw, fieldName);
    }

    public static <T> T readRequiredJsonBody(Context ctx, Class<T> clazz) {
        String raw = ctx.body();
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Request body is required");
        }
        try {
            return ctx.bodyAsClass(clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed JSON body");
        }
    }

    private static OffsetDateTime parseOffsetDateTimeOrThrow(String raw, String name) {
        try {
            return OffsetDateTime.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid '" + name + "' datetime. Use ISO-8601, e.g. 2026-02-21T20:30:00+01:00"
            );
        }
    }
}
