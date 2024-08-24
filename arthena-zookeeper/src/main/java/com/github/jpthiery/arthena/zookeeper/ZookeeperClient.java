package com.github.jpthiery.arthena.zookeeper;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;

class ZookeeperClient {

  private static final Logger LOGGER = getLogger(ZookeeperClient.class);

  protected final ZooKeeper zooKeeper;

  protected final ZnodePath rootZNode;

  ZookeeperClient(ZooKeeper zooKeeper, ZnodePath rootZNode) {
    requireNonNull(zooKeeper, "zooKeeper must be defined");
    this.zooKeeper = zooKeeper;
    this.rootZNode = rootZNode == null ? new ZnodePath("/arthena") : rootZNode;
  }

  ZookeeperClient(ZooKeeper zooKeeper) {
    this(zooKeeper, null);
  }

  public ZnodePath createZNode(ZnodePath path, byte[] data)
      throws InterruptedException, KeeperException {
    requireNonNull(path, "path must be defined");
    requireNonNull(data, "data must be defined");
    var accumulator = new StringBuilder();
    accumulator.append("/");
    var fullNode = path.withParent(rootZNode);
    var it = fullNode.pathSplit().iterator();
    while (it.hasNext()) {
      var item = it.next();
      accumulator.append(item);
      var stat = zooKeeper.exists(accumulator.toString(), false);
      if (stat == null) {
        if (it.hasNext()) {
          LOGGER.debug("Create zNode directory {}", accumulator);
          zooKeeper.create(
              accumulator.toString(),
              new byte[0],
              ZooDefs.Ids.OPEN_ACL_UNSAFE,
              CreateMode.PERSISTENT);
        } else {
          LOGGER.debug("Create zNode with value {}", accumulator);
          return new ZnodePath(
              zooKeeper.create(
                  accumulator.toString(),
                  data,
                  ZooDefs.Ids.OPEN_ACL_UNSAFE,
                  CreateMode.PERSISTENT));
        }
      }
      if (it.hasNext()) {
        accumulator.append("/");
      }
    }
    return new ZnodePath(accumulator.toString());
  }

  public ZnodePath createZNode(ZnodePath path) {
    try {
      createZNode(path, new byte[0]);
    } catch (InterruptedException | KeeperException e) {
      throw new ZooKeeperClientException("Unable to create Znode " + path.path(), e);
    }
    return path;
  }

  public void deleteZNode(ZnodePath path) throws InterruptedException, KeeperException {
    requireNonNull(path, "path must be defined");
    String pathToDelete = path.withParent(rootZNode).path();
    var exist = zooKeeper.exists(pathToDelete, false);
    if (exist != null) {
      zooKeeper.delete(pathToDelete, exist.getVersion());
    }
  }

  public List<ZnodePath> childrenPaths(ZnodePath path) {
    requireNonNull(path, "path must be defined");
    try {
      var childrenPath = zooKeeper.getChildren(path.withParent(rootZNode).path(), false);
      return childrenPath.stream()
          .map(item -> "/" + item)
          .map(ZnodePath::new)
          .map(item -> item.withParent(path))
          .toList();
    } catch (KeeperException | InterruptedException e) {
      throw new ZooKeeperClientException("Unable to list children node from " + path.path(), e);
    }
  }

  public Stat exist(ZnodePath path) {
    assert path != null;
    try {
      return zooKeeper.exists(path.withParent(rootZNode).path(), false);
    } catch (KeeperException | InterruptedException e) {
      throw new ZooKeeperClientException("Unable to check if node " + path.path() + " exist", e);
    }
  }

  public byte[] getContent(ZnodePath path) {
    requireNonNull(path, "path must be defined");
    var stat = exist(path);
    if (stat == null) {
      return new byte[0];
    }
    var currentPath = path.withParent(rootZNode);
    try {
      return zooKeeper.getData(currentPath.path(), false, stat);
    } catch (KeeperException | InterruptedException e) {
      throw new RuntimeException("Unable to retrieve content for path " + path.path(), e);
    }
  }

  public void update(ZnodePath path, byte[] newData) {
    requireNonNull(path, "path must be defined");
    requireNonNull(newData, "newData must be defined");
    var stat = exist(path);
    if (stat != null) {
      try {
        zooKeeper.setData(path.withParent(rootZNode).path(), newData, stat.getVersion());
      } catch (KeeperException | InterruptedException e) {
        throw new ZooKeeperClientException("Unable to update node " + path.path(), e);
      }
    }
  }

  public void deleteZNodeAndAllChildren(ZnodePath path) {
    Stat stat = exist(path);
    if (stat != null) {
      childrenPaths(path.withParent(rootZNode)).forEach(this::deleteZNodeAndAllChildren);
      try {
        zooKeeper.delete(path.withParent(rootZNode).path(), stat.getVersion());
      } catch (InterruptedException | KeeperException e) {
        throw new ZooKeeperClientException("Unable to delete node " + path.path(), e);
      }
    }
  }

  public Stat watchChangeOn(ZnodePath path, Watcher watcher) {
    try {
      return zooKeeper.exists(path.withParent(rootZNode).path(), watcher);
    } catch (KeeperException | InterruptedException e) {
      throw new RuntimeException("Unable to watch changes on node " + path.path(), e);
    }
  }

  public static class ZooKeeperClientException extends RuntimeException {
    public ZooKeeperClientException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
