package com.github.jpthiery.arthena;

import com.github.jpthiery.arthena.domain.Configuration;
import com.github.jpthiery.arthena.domain.ConfigurationEntry;
import com.github.jpthiery.arthena.domain.ConfigurationKey;
import com.github.jpthiery.arthena.domain.Environment;

public interface ConfigurationManager {

  void store(Configuration<?> configuration) throws ConfigurationAlreadyExist;

  <T> void defineValue(ConfigurationKey key, ConfigurationEntry<T> value, Class<T> tClass) throws ConfigurationNotFound;

  <T> void defineValue(ConfigurationKey key, Environment environment, ConfigurationEntry<T> value, Class<T> tClass)
      throws ConfigurationNotFound;

  void delete(ConfigurationKey key);

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

  class ConfigurationAlreadyExist extends Exception {

    private final ConfigurationKey key;

    public ConfigurationAlreadyExist(ConfigurationKey key) {
      super("Configuration with key " + key.key() + " already exist");
      this.key = key;
    }

    public ConfigurationKey getKey() {
      return key;
    }
  }
}
