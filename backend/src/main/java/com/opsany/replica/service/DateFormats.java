package com.opsany.replica.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateFormats {

    public static final DateTimeFormatter SECOND_PRECISION = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter MINUTE_PRECISION = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DateFormats() {
    }

    public static LocalDateTime parseFlexible(String value) {
        if (value == null || value.trim().isEmpty() || "--".equals(value)) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(value, SECOND_PRECISION);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value, MINUTE_PRECISION);
        } catch (Exception ignored) {
        }
        return LocalDateTime.now();
    }
}
