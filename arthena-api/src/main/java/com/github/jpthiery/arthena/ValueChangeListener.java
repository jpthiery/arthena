package com.github.jpthiery.arthena;

import com.github.jpthiery.arthena.domain.ConfigurationEntry;
import com.github.jpthiery.arthena.domain.ConfigurationKey;
import com.github.jpthiery.arthena.domain.Environment;

/**
 * Listener which should be notified when a value for a configuration changed. Listener should be
 * registered by subscribing to {@link
 * ConfigurationValueProvider#subscribeToValueChange(ConfigurationKey, ValueChangeListener, Class)}
 * or {@link ConfigurationValueProvider#subscribeToValueChange(ConfigurationKey, Environment,
 * ValueChangeListener, Class)} method.
 */
public interface ValueChangeListener {

  /**
   * Should be called when a value for a subscribed Configuration have changed.
   *
   * @param key The of the configuration which value change
   * @param previous The previous value of configuration, could be <code>null</code>
   * @param current The updated value of configuration, could be <code>null</code>
   * @param <T> Type of value provided by the Configuration
   */
  <T> void valueChange(
      ConfigurationKey key, ConfigurationEntry<T> previous, ConfigurationEntry<T> current);
}
