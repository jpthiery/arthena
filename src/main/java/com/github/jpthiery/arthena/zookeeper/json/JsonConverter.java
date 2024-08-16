package com.github.jpthiery.arthena.zookeeper.json;

import com.github.jpthiery.arthena.domain.Configuration;
import com.github.jpthiery.arthena.domain.ConfigurationEntry;

public interface JsonConverter {

  <T> Configuration<T> configurationFromJsonByte(byte[] jsonNode, Class<T> tClass);

  <T> ConfigurationEntry<T> configurationEntryFromJsonByte(byte[] jsonNode, Class<T> tClass);

  <T> byte[] toJsonByte(Configuration<T> configuration);

  <T> byte[] toJsonByte(ConfigurationEntry<T> configurationEntry);
}
