package com.github.jpthiery.arthena.zookeeper;

import static java.util.Objects.requireNonNull;

import com.github.jpthiery.arthena.domain.ConfigurationKey;
import com.github.jpthiery.arthena.domain.Environment;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public record ZnodePath(String path) {

  private static final Pattern VALID = Pattern.compile("/([a-zA-Z0-9])+(/([a-zA-Z0-9])+)*");

  public static ZnodePath from(ConfigurationKey configurationKey) {
    requireNonNull(configurationKey, "configurationKey must be defined");
    return new ZnodePath("/" + configurationKey.key().replace(ConfigurationKey.SEPARATOR, '/'));
  }

  public static ZnodePath from(Environment environment) {
    requireNonNull(environment, "environment must be defined");
    return new ZnodePath("/" + environment.name());
  }

  public ZnodePath {
    if (path == null || path.isBlank()) {
      throw new IllegalArgumentException("path must be defined and non empty");
    }
    var match = VALID.matcher(path);
    if (!match.matches()) {
      throw new IllegalArgumentException(
          "Znode path '" + path + "' not match pattern " + VALID.pattern());
    }
  }

  public List<String> pathSplit() {
    return Arrays.stream(path.split("/")).filter(item -> !item.isBlank()).toList();
  }

  public ZnodePath withParent(ZnodePath parent) {
    if (this.path.startsWith(parent.path())) {
      return this;
    }
    return new ZnodePath(parent.path + this.path);
  }
}
