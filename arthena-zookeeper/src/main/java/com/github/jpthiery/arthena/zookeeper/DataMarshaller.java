package com.github.jpthiery.arthena.zookeeper;

import com.github.jpthiery.arthena.domain.Configuration;
import com.github.jpthiery.arthena.domain.ConfigurationEntry;

/** Allow to marshall data entity stored in Zookeeper. */
public interface DataMarshaller {

  /**
   * Convert a byte array into a {@link Configuration} class
   *
   * @param data The source
   * @param tClass Type a Value provided by the configuration
   * @return A Configuration
   * @param <T> Type of value provided by Configuration
   */
  <T> Configuration<T> configurationFromByteArray(byte[] data, Class<T> tClass);

  /**
   * Convert a byte array into a {@link ConfigurationEntry} class
   *
   * @param data The source
   * @param tClass Type a Value provided by the configuration entry
   * @return A ConfigurationEntry
   * @param <T> Type of value provided by ConfigurationEntry
   */
  <T> ConfigurationEntry<T> configurationEntryFromByteArray(byte[] data, Class<T> tClass);

  /**
   * Convert a Configuration into a byte array
   *
   * @param configuration The source
   * @return a byte array which contain source
   * @param <T> Type of value provided by the configuration
   */
  <T> byte[] toByteArray(Configuration<T> configuration);

  /**
   * Convert a ConfigurationEntry into a byte array
   *
   * @param configurationEntry The source
   * @return a byte array which contain source
   * @param <T> Type of value provided by the configuration entry
   */
  <T> byte[] toByteArray(ConfigurationEntry<T> configurationEntry);
}
