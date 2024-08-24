package com.github.jpthiery.arthena.zookeeper.json;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.Function;

/** Provide a Function which allow to convert a JsonNode into a target value type */
public interface JsonNodeToValueConverterProvider {

  /**
   * For a given target class, provide a function which convert a JsonNote into that type.
   *
   * @param tClass The target class
   * @return A function to convert a JsonNode into the target class.
   * @param <T> The Type of the target class
   */
  <T> Function<JsonNode, T> provide(Class<T> tClass);
}
