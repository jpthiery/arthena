package com.github.jpthiery.arthena.zookeeper;

import static com.github.jpthiery.arthena.zookeeper.ZookeeperConfigurationManager.*;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;
import static org.slf4j.LoggerFactory.getLogger;

import com.github.jpthiery.arthena.ConfigurationKeyLockTemplate;
import com.github.jpthiery.arthena.ConfigurationValueProvider;
import com.github.jpthiery.arthena.ValueChangeListener;
import com.github.jpthiery.arthena.domain.ConfigurationEntry;
import com.github.jpthiery.arthena.domain.ConfigurationKey;
import com.github.jpthiery.arthena.domain.Environment;
import com.github.jpthiery.arthena.zookeeper.json.JsonDataMarshaller;
import java.util.*;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;

public class ZookeeperConfigurationValueProvider implements ConfigurationValueProvider {

  private static final Logger LOGGER = getLogger(ZookeeperConfigurationValueProvider.class);

  private final ZookeeperClient zookeeperClient;

  private final DataMarshaller dataMarshaller;

  private final ConfigurationKeyLockTemplate lockTemplate;

  public ZookeeperConfigurationValueProvider(ZooKeeper zookeeper, DataMarshaller dataMarshaller) {
    requireNonNull(zookeeper, "zookeeper must be defined");
    this.zookeeperClient = new ZookeeperClient(zookeeper);
    this.dataMarshaller = requireNonNullElseGet(dataMarshaller, JsonDataMarshaller::new);
    this.lockTemplate = new ConfigurationKeyLockTemplate();
  }

  public ZookeeperConfigurationValueProvider(ZooKeeper zookeeper) {
    this(zookeeper, null);
  }

  @Override
  public <T> Optional<T> getValue(ConfigurationKey key, Class<T> tClass) {
    return getValue(key, null, tClass);
  }

  @Override
  public <T> Optional<T> getValue(ConfigurationKey key, Environment environment, Class<T> tClass) {
    requireNonNull(key, "key must be defined");
    requireNonNull(tClass, "tClass must be defined");
    var keyPath = ZnodePath.from(key);
    try {
      return lockTemplate.operateWithLockOnConfiguration(
          key,
          () -> {
            var stat = zookeeperClient.exist(keyPath);
            if (stat == null) {
              return Optional.empty();
            }
            var configuration =
                dataMarshaller.configurationFromByteArray(
                    zookeeperClient.getContent(CONFIG.withParent(keyPath)), tClass);
            ZnodePath valuePath =
                environment == null
                    ? VALUE.withParent(keyPath)
                    : ZnodePath.from(environment).withParent(keyPath);
            var valueContent = zookeeperClient.getContent(valuePath);
            var configurationEntry =
                dataMarshaller.configurationEntryFromByteArray(valueContent, tClass);
            if (configurationEntry.value() == null) {
              return Optional.ofNullable(configuration.defaultVariant().value());
            }
            return Optional.ofNullable(configurationEntry.value());
          });
    } catch (Exception e) {
      throw new RuntimeException("Unable to get value for key " + key.key(), e);
    }
  }

  @Override
  public <T> void subscribeToValueChange(
      ConfigurationKey key, ValueChangeListener listener, Class<T> tClass) {
    subscribeToValueChange(key, null, listener, tClass);
  }

  @Override
  public <T> void subscribeToValueChange(
      ConfigurationKey key,
      Environment environment,
      ValueChangeListener listener,
      Class<T> tClass) {
    requireNonNull(listener, "listener must be defined");
    requireNonNull(tClass, "tClass must be defined");
    var path = ZnodePath.from(key);
    ZnodePath valuePath =
        environment == null ? VALUE.withParent(path) : ZnodePath.from(environment).withParent(path);
    new PreviousValueWatcher(
        zookeeperClient,
        valuePath,
        (path1, previous, newData) ->
            convertDataToConfigurationEntryAndNotify(key, listener, tClass, previous, newData));
  }

  private <T> void convertDataToConfigurationEntryAndNotify(
      ConfigurationKey key,
      ValueChangeListener listener,
      Class<T> tClass,
      byte[] previous,
      byte[] newData) {
    ConfigurationEntry<T> previousEntry = null;
    if (previous != null && previous.length > 0) {
      previousEntry = dataMarshaller.configurationEntryFromByteArray(previous, tClass);
    }
    ConfigurationEntry<T> currentEntry = null;
    if (newData != null && newData.length > 0) {
      currentEntry = dataMarshaller.configurationEntryFromByteArray(newData, tClass);
    }
    listener.valueChange(key, previousEntry, currentEntry);
  }

  private interface DataObserver {
    void change(ZnodePath path, byte[] previous, byte[] newData);
  }

  private static class PreviousValueWatcher implements Watcher {

    private final ZookeeperClient zookeeperClient;

    private final ZnodePath path;

    private final DataObserver dataObserver;

    private final byte[] previous;

    private PreviousValueWatcher(
        ZookeeperClient zookeeperClient, ZnodePath path, DataObserver dataObserver) {
      this.zookeeperClient = zookeeperClient;
      this.path = path;
      this.dataObserver = dataObserver;
      var stat = zookeeperClient.watchChangeOn(path, this);
      if (stat != null) {
        previous = zookeeperClient.getContent(path);
        LOGGER.debug("Subscribe to an existing data on node {}", path.path());
      } else {
        previous = new byte[0];
        LOGGER.debug("Subscribe to an empty a on node {}", path.path());
      }
    }

    private PreviousValueWatcher(
        ZookeeperClient zookeeperClient,
        ZnodePath path,
        DataObserver dataObserver,
        byte[] previous) {
      this.zookeeperClient = zookeeperClient;
      this.path = path;
      this.dataObserver = dataObserver;
      this.previous = previous;
      if (previous != null && previous.length > 0) {
        LOGGER.debug("Subscribe to an existing data on node {}", path.path());
      } else {
        LOGGER.debug("Subscribe to an empty a on node {}", path.path());
      }
      zookeeperClient.watchChangeOn(path, this);
    }

    @Override
    public void process(WatchedEvent event) {
      LOGGER.debug("Received event {} for listener instance {}", event, this.hashCode());
      if (event.getType() == Event.EventType.NodeDataChanged) {
        var current = zookeeperClient.getContent(path);
        try {
          dataObserver.change(path, previous, current);
        } catch (Exception e) {
          LOGGER.error("Unable to dispatch event {} to the data observer", event.getType(), e);
        }
        zookeeperClient.watchChangeOn(
            path, new PreviousValueWatcher(zookeeperClient, path, dataObserver, current));
      } else {
        if (event.getState() == Event.KeeperState.SyncConnected) {
          zookeeperClient.watchChangeOn(
              path, new PreviousValueWatcher(zookeeperClient, path, dataObserver));
        }
      }
    }
  }
}
