package com.github.jpthiery.arthena.zookeeper;

import com.github.jpthiery.arthena.domain.Configuration;
import com.github.jpthiery.arthena.domain.ConfigurationEntry;
import com.github.jpthiery.arthena.domain.ConfigurationKey;
import com.github.jpthiery.arthena.domain.Environment;
import java.util.Collections;
import java.util.List;

public interface ConfigurationFixture {

  ConfigurationEntry<Boolean> ON = new ConfigurationEntry<>("on", Boolean.TRUE, "Log is on");

  ConfigurationEntry<Boolean> OFF = new ConfigurationEntry<>("off", Boolean.FALSE, "Log is off");

  Environment DEV = new Environment("dev");

  Environment PROD = new Environment("prod");

  Configuration<Boolean> CONFIGURATION =
      new Configuration<>(
          new ConfigurationKey("my.app.awersome.loglevel"),
          "Loglevel",
          Collections.emptyMap(),
          List.of(ON, OFF),
          ON);
}
