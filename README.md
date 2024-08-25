# Arthena

Small tool to manage configuration for Java application.

___

## Why Arthena ?

On a project, i have the opportunity to explore Feature flag library "[Open Feature](https://openfeature.dev/)" and they
define a Feature flag as a small running lifecycle.
This is the difference between a Feature flag manager and a Configuration manager. It already
exists some configuration manager for Java, but I would like to have a small library which could
use [Zookeeper](https://zookeeper.apache.org/) as configuration repository.

## Features

As it a small tool, Arthena bring a small set of feature, but i hope, done it right :

* Store a Configuration definition
* Store a value for a given Configuration, optionally for a defined Environment
* Notify a listener when the Value changed for a Configuration

## Quickstart

### Maven

In your pom.xml dependency section, you should add :

```xml
<dependency>
    <groupId>io.github.jpthiery.arthena</groupId>
    <artifactId>arthena-api</artifactId>
    <version>0.1.2</version>
</dependency>
<dependency>
    <groupId>io.github.jpthiery.arthena</groupId>
    <artifactId>arthena-zookeeper</artifactId>
    <version>0.1.2</version>
</dependency>
```

### Sample of code 

```java
ConfigurationEntry<Boolean> on = new ConfigurationEntry<>("on", Boolean.TRUE, "Log is on");
ConfigurationEntry<Boolean> off = new ConfigurationEntry<>("off", Boolean.FALSE, "Log is off");

Configuration<Boolean> configuration =
        new Configuration<>(
                new ConfigurationKey("my.app.awersome.loglevel"),
                "Loglevel",
                Collections.emptyMap(),
                List.of(on, off),
                on);

var zooKeeper = new Zookeeper("localhost"); //  Zookeeper object provided by Zookeeper library
var configurationManager = new ZookeeperConfigurationManager(zooKeeper);

configurationManager.store(configuration);

var configurationProvider = new ZookeeperConfigurationValueProvider(zooKeeper);

var currentValue = configurationProvider.getValue(configuration.key());
System.out.println("Current value is " + currentValue.value());
```