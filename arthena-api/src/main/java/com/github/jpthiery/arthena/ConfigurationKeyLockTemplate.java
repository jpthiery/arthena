package com.github.jpthiery.arthena;

import static java.util.Objects.requireNonNull;

import com.github.jpthiery.arthena.domain.ConfigurationKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConfigurationKeyLockTemplate {

  private final Lock lockCreator = new ReentrantLock();

  private final Map<ConfigurationKey, Lock> configurationKeyLocks;

  public interface CallbackWithoutResult {
    void run() throws Exception;
  }

  public ConfigurationKeyLockTemplate() {
    configurationKeyLocks = new HashMap<>();
  }

  public <T> T operateWithLockOnConfiguration(ConfigurationKey key, Callable<T> callback)
      throws Exception {
    requireNonNull(key, "key must be defined");
    requireNonNull(callback, "callback must be defined");
    var lock = lockForConfigurationKey(key);
    lock.lock();
    try {
      return callback.call();
    } finally {
      lock.unlock();
    }
  }

  public void operateWithLockOnConfiguration(ConfigurationKey key, CallbackWithoutResult callback)
      throws Exception {
    requireNonNull(key, "key must be defined");
    requireNonNull(callback, "callback must be defined");
    var lock = lockForConfigurationKey(key);
    lock.lock();
    try {
      callback.run();
    } finally {
      lock.unlock();
    }
  }

  private Lock lockForConfigurationKey(ConfigurationKey key) {
    lockCreator.lock();
    try {
      return configurationKeyLocks.computeIfAbsent(key, item -> new ReentrantLock());
    } finally {
      lockCreator.unlock();
    }
  }
}
