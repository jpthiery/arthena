package com.github.jpthiery.arthena;

import com.github.jpthiery.arthena.domain.ConfigurationKey;
import com.github.jpthiery.arthena.domain.Environment;
import java.util.Optional;

/** Provide value for a given Configuration. */
public interface ConfigurationValueProvider {

  /**
   * Get value of a {@link com.github.jpthiery.arthena.domain.Configuration} in a given {@link
   * Environment}
   *
   * @param key The configuration key
   * @param environment The environment to look up the value
   * @param tClass Target class of the value expected
   * @return The value defined for a given configuration in a given environment.
   * @param <T> Type of value expected
   */
  <T> Optional<T> getValue(ConfigurationKey key, Environment environment, Class<T> tClass);

  /**
   * Get value of a {@link com.github.jpthiery.arthena.domain.Configuration}.
   *
   * @param key The configuration key
   * @param tClass Target class of the value expected
   * @return The value defined for a given configuration in a given environment.
   * @param <T> Type of value expected
   */
  <T> Optional<T> getValue(ConfigurationKey key, Class<T> tClass);

  /**
   * Subscribe a {@link ValueChangeListener} for a given Configuration on value defined for given
   * environment.
   *
   * @param key The configuration key
   * @param environment The environment to look up the value
   * @param listener The listener which should notify when the value changed
   * @param tClass Target class of the value
   * @param <T> Type of the value
   */
  <T> void subscribeToValueChange(
      ConfigurationKey key, Environment environment, ValueChangeListener listener, Class<T> tClass);

  /**
   * Subscribe a {@link ValueChangeListener} for a given Configuration.
   *
   * @param key The configuration key
   * @param listener The listener which should notify when the value changed
   * @param tClass Target class of the value
   * @param <T> Type of the value
   */
  <T> void subscribeToValueChange(
      ConfigurationKey key, ValueChangeListener listener, Class<T> tClass);
}
