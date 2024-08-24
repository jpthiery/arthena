package com.github.jpthiery.arthena.zookeeper;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;

public class ZookeeperProvider implements ParameterResolver, AfterEachCallback {

  private static final Logger LOGGER = getLogger(ZookeeperProvider.class);

  private static ZooKeeper ZOOKEEPER;

  private static int ZK_PORT = 0;

  private static ServerCnxnFactory ZK_SERVER;

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

  public ZooKeeper provideZookeeper() {
    LOCK.lock();
    try {
      if (ZOOKEEPER == null) {
        startZkServer();
        var latch = new CountDownLatch(1);
        var connectionString = "localhost:" + ZK_PORT;
        ZOOKEEPER =
            new ZooKeeper(
                connectionString,
                2000,
                event -> {
                  if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    latch.countDown();
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

  private Optional<String> getDefinedZookeeperUrl() {
    var testZookeeperUrl = System.getenv("TEST_ZOOKEEPER_URL");
    if (testZookeeperUrl != null && !testZookeeperUrl.isBlank()) {
      return Optional.of(testZookeeperUrl);
    }
    return Optional.empty();
  }

  private void startZkServer() throws IOException, InterruptedException {
    if (getDefinedZookeeperUrl().isEmpty()) {
      LOCK.lock();
      try {
        if (ZK_SERVER == null) {
          var dataDirectory = System.getProperty("java.io.tmpdir");
          var uuid = UUID.randomUUID();

          var dir = new File(dataDirectory, "test_zookeeper_" + uuid).getAbsoluteFile();

          var server = new ZooKeeperServer(dir, dir, 200);
          ZK_SERVER = ServerCnxnFactory.createFactory(0, 5000);
          ZK_PORT = ZK_SERVER.getLocalPort();
          LOGGER.info("Starting testing Zookeeper server on port {}", ZK_PORT);
          ZK_SERVER.startup(server);
        } else {
          LOGGER.info("Testing zookeeper already exist");
        }
      } finally {
        LOCK.unlock();
      }
    }
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    LOCK.lock();
    try {
      if (ZOOKEEPER != null) {
        ZOOKEEPER.close();
        ZOOKEEPER = null;
      }
      if (ZK_SERVER != null) {
        LOGGER.info("Stopping testing Zookeeper server on port {}", ZK_PORT);
        ZK_SERVER.shutdown();
        ZK_SERVER = null;
        ZK_PORT = 0;
      }
    } finally {
      LOCK.unlock();
    }
  }
}
