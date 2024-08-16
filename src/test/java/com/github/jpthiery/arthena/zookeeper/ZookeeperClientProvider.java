package com.github.jpthiery.arthena.zookeeper;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;

public class ZookeeperClientProvider implements ParameterResolver {

  private static final Logger LOGGER = getLogger(ZookeeperClientProvider.class);

  private static ZooKeeper ZOOKEEPER;

  private static final Lock LOCK = new ReentrantLock();

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return ZooKeeper.class.isAssignableFrom(parameterContext.getParameter().getType());
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return provideZookeeper();
  }

  private ZooKeeper provideZookeeper() {
    LOCK.lock();
    try {
      if (ZOOKEEPER == null) {
        var latch = new CountDownLatch(1);
        ZOOKEEPER =
            new ZooKeeper(
                "localhost",
                2000,
                new Watcher() {
                  @Override
                  public void process(WatchedEvent event) {
                    if (event.getState() == Event.KeeperState.SyncConnected) {
                      latch.countDown();
                    }
                  }
                });
        latch.await(5, TimeUnit.SECONDS);
        LOGGER.info("Zookeeper client created");
      }
      return ZOOKEEPER;
    } catch (InterruptedException | IOException e) {
      throw new RuntimeException("Unable to create a Zookeeper client", e);
    } finally {
      LOCK.unlock();
    }
  }
}
