package com.github.jpthiery.arthena.zookeeper;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import com.github.jpthiery.arthena.ConfigurationKeyLockTemplate;
import com.github.jpthiery.arthena.ConfigurationManager;
import com.github.jpthiery.arthena.domain.Configuration;
import com.github.jpthiery.arthena.domain.ConfigurationEntry;
import com.github.jpthiery.arthena.domain.ConfigurationKey;
import com.github.jpthiery.arthena.domain.Environment;
import com.github.jpthiery.arthena.zookeeper.json.JsonDataMarshaller;
import java.util.*;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;

public class ZookeeperConfigurationManager implements ConfigurationManager {

  private static final Logger LOGGER = getLogger(ZookeeperConfigurationManager.class);

  static final ZnodePath CONFIG = new ZnodePath("/config");

  static final ZnodePath VALUE = new ZnodePath("/value");

  private final ZookeeperClient zookeeperClient;

  private final DataMarshaller dataMarshaller;

  private final ConfigurationKeyLockTemplate lockTemplate;

  public ZookeeperConfigurationManager(ZooKeeper zooKeeper, DataMarshaller dataMarshaller) {
    requireNonNull(zooKeeper, "zooKeeper must be defined");
    this.zookeeperClient = new ZookeeperClient(zooKeeper);
    this.dataMarshaller = Objects.requireNonNullElseGet(dataMarshaller, JsonDataMarshaller::new);
    this.lockTemplate = new ConfigurationKeyLockTemplate();
  }

  public ZookeeperConfigurationManager(ZooKeeper zooKeeper) {
    this(zooKeeper, null);
  }

  @Override
  public void store(Configuration<?> configuration) {
    requireNonNull(configuration, "configuration must be defined");
    var key = configuration.key();
    try {
      lockTemplate.operateWithLockOnConfiguration(
          key,
          () -> {
            var paths = ZnodePath.from(key);
            var configData = dataMarshaller.toByteArray(configuration);
            var stat = zookeeperClient.exist(CONFIG.withParent(paths));
            if (stat != null) {
              zookeeperClient.update(paths, configData);
            } else {
              var valueData = dataMarshaller.toByteArray(configuration.defaultVariant());
              var rootConfigPath = zookeeperClient.createZNode(paths);
              zookeeperClient.createZNode(CONFIG.withParent(rootConfigPath), configData);
              zookeeperClient.createZNode(VALUE.withParent(rootConfigPath), valueData);
            }
          });
    } catch (Exception e) {
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
    try {
      lockTemplate.operateWithLockOnConfiguration(
          key,
          () -> {
            var stat = zookeeperClient.exist(path);
            if (stat == null) {
              throw new ConfigurationNotFound(key);
            }
            checkConfigurationEntryIsValid(key, value, tClass);
            var valuePath = VALUE.withParent(path);
            zookeeperClient.update(valuePath, dataMarshaller.toByteArray(value));
          });
    } catch (Exception e) {
      if (e instanceof ConfigurationNotFound notFound) {
        throw notFound;
      }
      throw new RuntimeException("Unable to define value for configuration " + key.key(), e);
    }
  }

  @Override
  public <T> void defineValue(
      ConfigurationKey key, Environment environment, ConfigurationEntry<T> value, Class<T> tClass)
      throws ConfigurationNotFound {
    requireNonNull(key, "key must be defined");
    requireNonNull(environment, "environment must be defined");
    requireNonNull(value, "value must be defined");
    var path = ZnodePath.from(key);
    try {
      lockTemplate.operateWithLockOnConfiguration(
          key,
          () -> {
            var stat = zookeeperClient.exist(path);
            if (stat == null) {
              throw new ConfigurationNotFound(key);
            }
            checkConfigurationEntryIsValid(key, value, tClass);
            var valuePath = ZnodePath.from(environment).withParent(path);
            byte[] data = dataMarshaller.toByteArray(value);
            var valueStat = zookeeperClient.exist(valuePath);
            if (valueStat == null) {
              zookeeperClient.createZNode(valuePath, data);
            } else {
              zookeeperClient.update(valuePath, data);
            }
          });

    } catch (Exception e) {
      if (e instanceof ConfigurationNotFound notFound) {
        throw notFound;
      } else if (e instanceof IllegalArgumentException notValidEntry) {
        throw notValidEntry;
      }
      throw new RuntimeException("Unable to define value for configuration " + key.key(), e);
    }
  }

  @Override
  public void delete(ConfigurationKey key) {
    requireNonNull(key, "key must be defined");
    var path = ZnodePath.from(key);
    zookeeperClient.deleteZNodeAndAllChildren(path);
  }

  @Override
  public List<Configuration<?>> list() {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private <T> void checkConfigurationEntryIsValid(
      ConfigurationKey key, ConfigurationEntry<T> configurationEntry, Class<T> tClass)
      throws ConfigurationNotFound {
    var keyPath = ZnodePath.from(key);
    var stat = zookeeperClient.exist(keyPath);
    if (stat == null) {
      throw new ConfigurationNotFound(key);
    }
    var configurationContent = zookeeperClient.getContent(CONFIG.withParent(keyPath));
    Configuration<?> configuration =
        dataMarshaller.configurationFromByteArray(configurationContent, tClass);
    if (!configuration.variants().contains(configurationEntry)) {
      throw new IllegalArgumentException("ConfigurationEntry not eligible to key " + key.key());
    }
  }
}
