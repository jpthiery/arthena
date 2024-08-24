package com.github.jpthiery.arthena.zookeeper;

import static java.util.Objects.requireNonNull;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.nio.charset.StandardCharsets;

public class StatAsserter extends AbstractAssert<StatAsserter, Stat> {

  private final ZooKeeper zooKeeper;

  private final ZnodePath znodePath;

  public static StatAsserter assertThat(ZooKeeper zooKeeper, Stat actual, ZnodePath znodePath) {
    return new StatAsserter(zooKeeper, actual, znodePath);
  }

  StatAsserter(ZooKeeper zooKeeper, Stat actual, ZnodePath znodePath) {
    super(actual, StatAsserter.class);
    requireNonNull(zooKeeper, "zooKeeper must be defined");
    requireNonNull(znodePath, "znodePath must be defined");
    this.znodePath = znodePath;
    this.zooKeeper = zooKeeper;
  }

  public StatAsserter hasValue(byte[] expectedContent) {
    try {
      var content = new String(zooKeeper.getData(znodePath.path(), false, actual), StandardCharsets.UTF_8);
      var expected = new String(expectedContent, StandardCharsets.UTF_8);
      Assertions.assertThat(content).as("Expecting content of node match").isEqualTo(expected);
    } catch (KeeperException | InterruptedException e) {
      throw new RuntimeException("Unable to fetch node content for path " + znodePath.path(), e);
    }
  return this;
  }

}
