package com.github.jpthiery.arthena.zookeeper.json;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.function.Function;

public interface JsonNodeToValueConverterProvider {

    <T> Function<JsonNode, T> provide(Class<T> tClass);

}
