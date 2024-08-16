package com.github.jpthiery.arthena.domain;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;

public record Configuration<T>(
    ConfigurationKey key,
    String name,
    Map<String, String> metadata,
    List<ConfigurationEntry<T>> variants,
    ConfigurationEntry<T> defaultVariant) {

  public Configuration {
    requireNonNull(key, "key must be defined");
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("key must be defined and non empty");
    }
    requireNonNull(metadata, "metadata must be defined");
    if (variants == null || variants.isEmpty()) {
      throw new IllegalArgumentException("variants must be defined and non empty");
    }
    requireNonNull(defaultVariant, "defaultVariant must be defined");
    if (!variants.contains(defaultVariant)) {
      throw new IllegalArgumentException("default variant is not part of listof variants");
    }
  }
}
