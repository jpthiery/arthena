package com.github.jpthiery.arthena.zookeeper;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.assertj.core.api.AbstractAssert;
import org.opentest4j.AssertionFailedError;

public class ZookeeperAsserter extends AbstractAssert<ZookeeperAsserter, ZooKeeper> {

  public static ZookeeperAsserter assertThat(ZooKeeper actual) {
    return new ZookeeperAsserter(actual);
  }

  private ZookeeperAsserter(ZooKeeper actual) {
    super(actual, ZookeeperAsserter.class);
  }

  public StatAsserter zNodeExist(String path) {
    try {
      var exist = actual.exists(path, false);
      if (exist == null) {
        throw failure("Expecting zNode " + path + " exist");
      }
      return new StatAsserter(actual, exist, new ZnodePath(path));
    } catch (KeeperException | InterruptedException e) {
      throw new AssertionFailedError("Unable to lookup if zNode " + path + "exist", e);
    }
  }

  public StatAsserter zNodeExist(ZnodePath path) {
    try {
      var exist = actual.exists(path.path(), false);
      if (exist == null) {
        throw failure("Expecting zNode " + path.path() + " exist");
      }
      return new StatAsserter(actual, exist, path);
    } catch (KeeperException | InterruptedException e) {
      throw new AssertionFailedError("Unable to lookup if zNode " + path + "exist", e);
    }
  }
}
