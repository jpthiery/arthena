package com.github.jpthiery.arthena.zookeeper;

import static com.github.jpthiery.arthena.zookeeper.ZookeeperAsserter.assertThat;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import com.github.jpthiery.arthena.ConfigurationManager;
import com.github.jpthiery.arthena.domain.ConfigurationEntry;
import java.nio.charset.StandardCharsets;
import org.apache.zookeeper.ZooKeeper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

@ExtendWith(ZookeeperProvider.class)
public class ZookeeperConfigurationManagerTest implements ConfigurationFixture {

  private static final Logger LOGGER = getLogger(ZookeeperConfigurationManagerTest.class);

  private final ZooKeeper zookeeper;

  private final ZookeeperConfigurationManager sut;

  public ZookeeperConfigurationManagerTest(ZooKeeper zookeeper) {
    requireNonNull(zookeeper, "zookeeper must be defined");
    this.zookeeper = zookeeper;
    this.sut = new ZookeeperConfigurationManager(zookeeper);
  }

  @AfterEach
  public void tearDown() {
    new ZookeeperClient(zookeeper).deleteZNodeAndAllChildren(new ZnodePath("/arthena"));
  }

  @Test
  public void itShouldStoreConfiguration() {

    sut.store(CONFIGURATION);

    assertThat(zookeeper)
        .zNodeExist("/arthena/my/app/awersome/loglevel/config")
        .hasValue(
            """
{
  "key" : {
    "key" : "my.app.awersome.loglevel"
  },
  "name" : "Loglevel",
  "metadata" : { },
  "variants" : [ {
    "name" : "on",
    "value" : true,
    "description" : "Log is on"
  }, {
    "name" : "off",
    "value" : false,
    "description" : "Log is off"
  } ],
  "defaultVariant" : {
    "name" : "on",
    "value" : true,
    "description" : "Log is on"
  }
}"""
                .getBytes(StandardCharsets.UTF_8));
    assertThat(zookeeper)
        .zNodeExist("/arthena/my/app/awersome/loglevel/value")
        .hasValue(
            """
{
  "name" : "on",
  "value" : true,
  "description" : "Log is on"
}"""
                .getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void itShouldUpdateConfigurationValue() throws ConfigurationManager.ConfigurationNotFound {

    sut.store(CONFIGURATION);

    sut.defineValue(CONFIGURATION.key(), OFF, Boolean.class);

    assertThat(zookeeper)
        .zNodeExist("/arthena/my/app/awersome/loglevel/value")
        .hasValue(
            """
        {
          "name" : "off",
          "value" : false,
          "description" : "Log is off"
        }"""
                .getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void itShouldUpdateConfigurationValueForDev()
      throws ConfigurationManager.ConfigurationNotFound {

    sut.store(CONFIGURATION);

    sut.defineValue(CONFIGURATION.key(), DEV, OFF, Boolean.class);

    assertThat(zookeeper)
        .zNodeExist("/arthena/my/app/awersome/loglevel/dev")
        .hasValue(
            """
        {
          "name" : "off",
          "value" : false,
          "description" : "Log is off"
        }"""
                .getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void itShouldNotUpdateConfigurationEntryNotDefined()
      throws ConfigurationManager.ConfigurationNotFound {

    sut.store(CONFIGURATION);
    Assertions.assertThatThrownBy(
            () -> {
              sut.defineValue(
                  CONFIGURATION.key(),
                  DEV,
                  new ConfigurationEntry<>(
                      "fake", Boolean.FALSE, "Unexpected " + "configuration entry"),
                  Boolean.class);
            })
        .isInstanceOf(IllegalArgumentException.class);
  }
}
