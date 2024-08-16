package com.github.jpthiery.arthena.zookeeper.json;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jpthiery.arthena.domain.Configuration;
import com.github.jpthiery.arthena.domain.ConfigurationEntry;
import com.github.jpthiery.arthena.domain.ConfigurationKey;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class DefaultJsonConverter implements JsonConverter {

  private final ObjectMapper objectMapper;

  private final JsonNodeToValueConverterProvider jsonNodeToValueConverterProvider;

  public DefaultJsonConverter(JsonNodeToValueConverterProvider jsonNodeToValueConverterProvider) {
    this.objectMapper = new ObjectMapper();
    this.jsonNodeToValueConverterProvider =
        Objects.requireNonNullElseGet(
            jsonNodeToValueConverterProvider, DefaultJsonNodeToValueConverterProvider::new);
  }

  public DefaultJsonConverter() {
    this(null);
  }

  @Override
  public <T> Configuration<T> configurationFromJsonByte(byte[] data, Class<T> tClass) {
    if (data == null || data.length == 0) {
      return null;
    }
    var objectMapper = new ObjectMapper();
    try {
      var json = objectMapper.readTree(data);
      var keyStr = json.get("key").get("key").asText();
      var name = json.get("name").asText();
      var metadata = new HashMap<String, String>();
      json.get("metadata")
          .fields()
          .forEachRemaining(field -> metadata.put(field.getKey(), field.getValue().asText()));
      var defaultVariant = jsonToConfigurationEntry(json.get("defaultVariant"), tClass);
      var variants = new ArrayList<ConfigurationEntry<T>>();
      json.get("variants")
          .iterator()
          .forEachRemaining(variant -> variants.add(jsonToConfigurationEntry(variant, tClass)));
      return new Configuration<>(
          new ConfigurationKey(keyStr), name, metadata, variants, defaultVariant);
    } catch (IOException e) {
      throw new RuntimeException("Unable to deserialize as Json configuration", e);
    }
  }

  @Override
  public <T> ConfigurationEntry<T> configurationEntryFromJsonByte(byte[] data, Class<T> tClass) {
    try {
      var jsonNode = objectMapper.readTree(data);
      return jsonToConfigurationEntry(jsonNode, tClass);
    } catch (IOException e) {
      throw new RuntimeException("Unable to read data", e);
    }
  }

  @Override
  public <T> byte[] toJsonByte(Configuration<T> configuration) {
    requireNonNull(configuration, "configuration must be defined");
    return objectToJsonBytes(configuration);
  }

  @Override
  public <T> byte[] toJsonByte(ConfigurationEntry<T> configurationEntry) {
    requireNonNull(configurationEntry, "configurationEntry must be defined");
    return objectToJsonBytes(configurationEntry);
  }

  private byte[] objectToJsonBytes(Object data) {
    if (data == null) {
      return new byte[0];
    }
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Unable to create a json data representation", e);
    }
  }

  private <T> ConfigurationEntry<T> jsonToConfigurationEntry(JsonNode jsonNode, Class<T> tClass) {
    var name = jsonNode.get("name").asText();
    var description = jsonNode.get("description").asText();
    var valueConverter = jsonNodeToValueConverterProvider.provide(tClass);
    var value = valueConverter.apply(jsonNode.get("value"));
    return new ConfigurationEntry<>(name, value, description);
  }
}
