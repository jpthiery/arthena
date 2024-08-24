package com.github.jpthiery.arthena.domain;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public record ConfigurationKey(String key) {

    public static final Character SEPARATOR = '.';

    private static final Pattern VALID = Pattern.compile("[a-zA-Z0-9]+(\\.([a-zA-Z0-9])+)*");

    public ConfigurationKey {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must be defined and non empty");
        }
        var match = VALID.matcher(key);
        if (!match.matches()) {
            throw new IllegalArgumentException("key " + key + " must match regex " + VALID.pattern());
        }
    }

    public List<String> toPaths() {
        var split = key.split("\\.");
        return Arrays.stream(split).toList();
    }
}
