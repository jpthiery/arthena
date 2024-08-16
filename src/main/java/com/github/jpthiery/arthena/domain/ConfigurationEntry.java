package com.github.jpthiery.arthena.domain;

public record ConfigurationEntry<T>(String name, T value, String description) {

  public ConfigurationEntry {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("key must be defined and non empty");
    }
  }
}
