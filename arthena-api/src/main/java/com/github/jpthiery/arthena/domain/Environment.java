package com.github.jpthiery.arthena.domain;

import java.util.regex.Pattern;

public record Environment(String name) {

  private static final Pattern VALID = Pattern.compile("[a-zA-Z0-9-_.]{3,}");

  public Environment {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must be defined and non empty");
    }
    if (name.length() < 3) {
      throw new IllegalArgumentException("Environment name must contain at least 3 characters");
    }
    var match = VALID.matcher(name);
    if (!match.matches()) {
      throw new IllegalArgumentException(
          "Environment name " + name + " not match pattern " + VALID.pattern());
    }
  }
}
