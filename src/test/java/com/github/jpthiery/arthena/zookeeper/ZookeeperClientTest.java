package com.github.jpthiery.arthena.zookeeper;

import static java.util.Objects.requireNonNull;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ZookeeperClientProvider.class)
class ZookeeperClientTest {

  private final ZooKeeper zooKeeper;

  private final ZookeeperClient sut;

  ZookeeperClientTest(ZooKeeper zooKeeper) {
    requireNonNull(zooKeeper, "zooKeeper must be defined");
    this.zooKeeper = zooKeeper;
    this.sut = new ZookeeperClient(zooKeeper);
  }

  @AfterEach
  void tearDown() throws InterruptedException, KeeperException {
    sut.deleteZNodeAndAllChildren(new ZnodePath("/arthena"));
  }

  @Test
  public void itShouldCreateNodeOnlyOnce() throws InterruptedException, KeeperException {

    var nodePath = sut.createZNode(new ZnodePath("/a/b/c"));
    var actual = sut.createZNode(new ZnodePath("/a/b/c"));
    Assertions.assertThat(actual).isEqualTo(nodePath);
  }
}
