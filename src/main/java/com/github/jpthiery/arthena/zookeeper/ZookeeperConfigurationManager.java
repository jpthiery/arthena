package com.github.jpthiery.arthena.zookeeper;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import com.github.jpthiery.arthena.ConfigurationManager;
import com.github.jpthiery.arthena.domain.Configuration;
import com.github.jpthiery.arthena.domain.ConfigurationEntry;
import com.github.jpthiery.arthena.domain.ConfigurationKey;
import com.github.jpthiery.arthena.domain.Environment;
import com.github.jpthiery.arthena.zookeeper.json.DefaultJsonConverter;
import com.github.jpthiery.arthena.zookeeper.json.JsonConverter;
import java.util.*;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;

public class ZookeeperConfigurationManager implements ConfigurationManager {

  private static final Logger LOGGER = getLogger(ZookeeperConfigurationManager.class);

  static final ZnodePath CONFIG = new ZnodePath("/config");

  static final ZnodePath VALUE = new ZnodePath("/value");

  private final ZookeeperClient zookeeperClient;

  private final JsonConverter jsonConverter;

  public ZookeeperConfigurationManager(ZooKeeper zooKeeper, JsonConverter jsonConverter) {
    requireNonNull(zooKeeper, "zooKeeper must be defined");
    this.zookeeperClient = new ZookeeperClient(zooKeeper);
    this.jsonConverter = Objects.requireNonNullElseGet(jsonConverter, DefaultJsonConverter::new);
  }

  public ZookeeperConfigurationManager(ZooKeeper zooKeeper) {
    this(zooKeeper, null);
  }

  @Override
  public void store(Configuration<?> configuration) throws ConfigurationAlreadyExist {
    requireNonNull(configuration, "configuration must be defined");
    var key = configuration.key();
    var paths = ZnodePath.from(key);
    var stat = zookeeperClient.exist(CONFIG.withParent(paths));
    if (stat != null) {
      throw new ConfigurationAlreadyExist(configuration.key());
    }
    var rootConfigPath = zookeeperClient.createZNode(paths);
    try {
      var configData = jsonConverter.toJsonByte(configuration);
      var valueData = jsonConverter.toJsonByte(configuration.defaultVariant());
      zookeeperClient.createZNode(CONFIG.withParent(rootConfigPath), configData);
      zookeeperClient.createZNode(VALUE.withParent(rootConfigPath), valueData);
    } catch (InterruptedException | KeeperException e) {
      throw new RuntimeException(
          "An error occurred while trying to store configuration key "
              + key.key()
              + " to store the configuration data",
          e);
    }
  }

  @Override
  public <T> void defineValue(ConfigurationKey key, ConfigurationEntry<T> value, Class<T> tClass)
      throws ConfigurationNotFound {
    requireNonNull(key, "key must be defined");
    requireNonNull(value, "value must be defined");
    var path = ZnodePath.from(key);
    var stat = zookeeperClient.exist(path);
    if (stat == null) {
      throw new ConfigurationNotFound(key);
    }
    checkConfigurationEntryIsValid(key, value, tClass);
    var valuePath = VALUE.withParent(path);
    zookeeperClient.update(valuePath, jsonConverter.toJsonByte(value));
  }

  @Override
  public <T> void defineValue(
      ConfigurationKey key, Environment environment, ConfigurationEntry<T> value, Class<T> tClass)
      throws ConfigurationNotFound {
    requireNonNull(key, "key must be defined");
    requireNonNull(environment, "environment must be defined");
    requireNonNull(value, "value must be defined");
    var path = ZnodePath.from(key);
    var stat = zookeeperClient.exist(path);
    if (stat == null) {
      throw new ConfigurationNotFound(key);
    }
    checkConfigurationEntryIsValid(key, value, tClass);
    var valuePath = ZnodePath.from(environment).withParent(path);
    try {
      byte[] data = jsonConverter.toJsonByte(value);
      var valueStat = zookeeperClient.exist(valuePath);
      if (valueStat == null) {
        zookeeperClient.createZNode(valuePath, data);
      } else {
        zookeeperClient.update(valuePath, data);
      }
    } catch (InterruptedException | KeeperException e) {
      throw new RuntimeException(
          "Unable to create a new Environment value for env "
              + environment.name()
              + " for "
              + "configuration "
              + key.key(),
          e);
    }
  }

  private <T> void checkConfigurationEntryIsValid(
      ConfigurationKey key, ConfigurationEntry<T> configurationEntry, Class<T> tClass) {
    var keyPath = ZnodePath.from(key);
    var stat = zookeeperClient.exist(keyPath);
    if (stat == null) {
      throw new IllegalArgumentException("Unable to found configuration for key " + key.key());
    }
    var configurationContent = zookeeperClient.getContent(CONFIG.withParent(keyPath));
    Configuration<?> configuration =
        jsonConverter.configurationFromJsonByte(configurationContent, tClass);
    if (!configuration.variants().contains(configurationEntry)) {
      throw new IllegalArgumentException("ConfigurationEntry not eligible to key " + key.key());
    }
  }

  @Override
  public void delete(ConfigurationKey key) {
    requireNonNull(key, "key must be defined");
    var path = ZnodePath.from(key);
    zookeeperClient.deleteZNodeAndAllChildren(path);
  }
}
