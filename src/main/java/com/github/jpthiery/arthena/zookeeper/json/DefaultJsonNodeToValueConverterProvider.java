package com.github.jpthiery.arthena.zookeeper.json;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.Function;

public class DefaultJsonNodeToValueConverterProvider implements JsonNodeToValueConverterProvider {
  @Override
  public <T> Function<JsonNode, T> provide(Class<T> tClass) {
    requireNonNull(tClass, "tClass must be defined");
    if (Boolean.class.isAssignableFrom(tClass)) {
      return jsonNode -> (T) Boolean.valueOf(jsonNode.asBoolean());
    } else if (Integer.class.isAssignableFrom(tClass)) {
      return jsonNode -> (T) Integer.valueOf(jsonNode.asInt());
    } else if (Long.class.isAssignableFrom(tClass)) {
      return jsonNode -> (T) Long.valueOf(jsonNode.asLong());
    } else if (Double.class.isAssignableFrom(tClass)) {
      return jsonNode -> (T) Double.valueOf(jsonNode.asDouble());
    } else if (String.class.isAssignableFrom(tClass)) {
      return jsonNode -> (T) jsonNode.asText();
    }
    return null;
  }
}
