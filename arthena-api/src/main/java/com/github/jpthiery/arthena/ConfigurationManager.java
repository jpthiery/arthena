package com.github.jpthiery.arthena;

import com.github.jpthiery.arthena.domain.Configuration;
import com.github.jpthiery.arthena.domain.ConfigurationEntry;
import com.github.jpthiery.arthena.domain.ConfigurationKey;
import com.github.jpthiery.arthena.domain.Environment;

import java.util.List;

/** Allow to store, delete and define value of a {@link Configuration}. */
public interface ConfigurationManager {

  /**
   * Persist a Configuration.
   *
   * @param configuration The configuration to store
   */
  void store(Configuration<?> configuration);

  /**
   * Define the value to a Configuration.
   *
   * @param key The configuration key which value must be defined
   * @param value The value to define
   * @param tClass Target class of the ConfigurationEntry
   * @param <T> Type of value
   * @throws ConfigurationNotFound if the configuration key is not found.
   */
  <T> void defineValue(ConfigurationKey key, ConfigurationEntry<T> value, Class<T> tClass)
      throws ConfigurationNotFound;

  /**
   * Define the value to a Configuration for specific {@link Environment}.
   *
   * @param key The configuration key which value must be defined
   * @param environment The environment to affect the value.
   * @param value The value to define
   * @param tClass Target class of the ConfigurationEntry
   * @param <T> Type of value
   * @throws ConfigurationNotFound if the configuration key is not found.
   */
  <T> void defineValue(
      ConfigurationKey key, Environment environment, ConfigurationEntry<T> value, Class<T> tClass)
      throws ConfigurationNotFound;

  /**
   * Delete a configuration.
   *
   * @param key The configuration key to delete
   */
  void delete(ConfigurationKey key);

  List<Configuration<?>> list();

  class ConfigurationNotFound extends Exception {

    private final ConfigurationKey key;

    public ConfigurationNotFound(ConfigurationKey key) {
      super("Unable to found configuration with key " + key.key());
      this.key = key;
    }

    public ConfigurationKey getKey() {
      return key;
    }
  }
}
