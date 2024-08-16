package com.github.jpthiery.arthena.zookeeper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jpthiery.arthena.ConfigurationManager;
import com.github.jpthiery.arthena.ValueChangeListener;
import com.github.jpthiery.arthena.domain.ConfigurationEntry;
import com.github.jpthiery.arthena.domain.ConfigurationKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

@ExtendWith(ZookeeperClientProvider.class)
class ZookeeperConfigurationValueProviderTest implements ConfigurationFixture {

  private static final Logger LOGGER = getLogger(ZookeeperConfigurationValueProviderTest.class);

  private final ZooKeeper zooKeeper;

  private final ZookeeperConfigurationValueProvider sut;

  private final ZookeeperConfigurationManager configurationManager;

  private final ObjectMapper objectMapper;

  ZookeeperConfigurationValueProviderTest(ZooKeeper zooKeeper) {
    this.zooKeeper = zooKeeper;
    this.sut = new ZookeeperConfigurationValueProvider(zooKeeper, null);
    this.configurationManager = new ZookeeperConfigurationManager(zooKeeper);
    this.objectMapper = new ObjectMapper();
  }

  @AfterEach
  public void tearDown() {
    new ZookeeperClient(this.zooKeeper).deleteZNodeAndAllChildren(new ZnodePath("/arthena"));
  }

  @BeforeEach
  public void cleanBefore() {
    new ZookeeperClient(this.zooKeeper).deleteZNodeAndAllChildren(new ZnodePath("/arthena"));
  }

  @Test
  public void itShouldNotifyWhenConfigurationChanged()
      throws ConfigurationManager.ConfigurationAlreadyExist,
          JsonProcessingException,
          InterruptedException,
          KeeperException {
    configurationManager.store(CONFIGURATION);

    var spyListener = new SpyListener(1);
    sut.subscribeToValueChange(CONFIGURATION.key(), spyListener, Boolean.class);

    var newValue = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(OFF);
    var keyPath = ZnodePath.from(CONFIGURATION.key());
    var znodePath =
        ZookeeperConfigurationManager.VALUE
            .withParent(keyPath)
            .withParent(new ZnodePath("/arthena"));

    var stat = zooKeeper.exists(znodePath.path(), false);

    LOGGER.debug("Updating znode " + znodePath.path());
    zooKeeper.setData(znodePath.path(), newValue, stat.getVersion());

    spyListener.await();
    assertThat(spyListener.changes).hasSize(1);
    Change<?> actualChange = spyListener.changes.getFirst();
    LOGGER.debug("Change {}", actualChange);
    assertThat(actualChange).isNotNull();
    assertThat(actualChange.key).isEqualTo(CONFIGURATION.key());
    assertThat(actualChange.previous).isEqualTo(ON);
    assertThat(actualChange.current).isEqualTo(OFF);
  }

  @Test
  public void itShouldReWatchWhenConfigurationChanged()
      throws ConfigurationManager.ConfigurationAlreadyExist,
          JsonProcessingException,
          InterruptedException,
          KeeperException {
    configurationManager.store(CONFIGURATION);

    var spyListener = new SpyListener(2);
    sut.subscribeToValueChange(CONFIGURATION.key(), spyListener, Boolean.class);

    var oldValue = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(ON);
    var newValue = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(OFF);
    var keyPath = ZnodePath.from(CONFIGURATION.key());
    var znodePath =
        ZookeeperConfigurationManager.VALUE
            .withParent(keyPath)
            .withParent(new ZnodePath("/arthena"));

    Stat stat = zooKeeper.exists(znodePath.path(), false);

    LOGGER.debug("Updating znode " + znodePath.path());
    zooKeeper.setData(znodePath.path(), newValue, stat.getVersion());
    spyListener.awaitNbChange(1);
    stat = zooKeeper.exists(znodePath.path(), false);
    zooKeeper.setData(znodePath.path(), oldValue, stat.getVersion());

    spyListener.await();
    assertThat(spyListener.changes).hasSize(2);
    Change<?> firstChange = spyListener.changes.getFirst();
    LOGGER.debug("Change {}", firstChange);
    assertThat(firstChange).isNotNull();
    assertThat(firstChange.key).isEqualTo(CONFIGURATION.key());
    assertThat(firstChange.previous).isEqualTo(ON);
    assertThat(firstChange.current).isEqualTo(OFF);
    Change<?> secondChange = spyListener.changes.get(1);
    LOGGER.debug("Change {}", secondChange);
    assertThat(secondChange).isNotNull();
    assertThat(secondChange.key).isEqualTo(CONFIGURATION.key());
    assertThat(secondChange.previous).isEqualTo(OFF);
    assertThat(secondChange.current).isEqualTo(ON);
  }

  private record Change<T>(
      ConfigurationKey key, ConfigurationEntry<T> previous, ConfigurationEntry<T> current) {}

  private static class SpyListener implements ValueChangeListener {

    private final List<Change<?>> changes = new ArrayList<>();

    private final List<CountDownLatch> countDownLatchs;

    private final CountDownLatch initialLatch;

    public SpyListener(int eventWaitBeforeLatch) {
      countDownLatchs = new ArrayList<>();
      initialLatch = new CountDownLatch(eventWaitBeforeLatch);
      countDownLatchs.add(initialLatch);
    }

    @Override
    public <T> void valueChange(
        ConfigurationKey key, ConfigurationEntry<T> previous, ConfigurationEntry<T> current) {
      changes.add(new Change<>(key, previous, current));
      countDownLatchs.forEach(CountDownLatch::countDown);
    }

    public void await() throws InterruptedException {
      initialLatch.await(1, TimeUnit.SECONDS);
    }

    public void awaitNbChange(int expectedNbChange) throws InterruptedException {
      var specLatch = new CountDownLatch(expectedNbChange);
      countDownLatchs.add(specLatch);
      specLatch.await(1, TimeUnit.SECONDS);
    }
  }
}
