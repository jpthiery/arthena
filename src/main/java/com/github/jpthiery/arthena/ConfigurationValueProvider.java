package com.github.jpthiery.arthena;

import com.github.jpthiery.arthena.domain.ConfigurationKey;
import com.github.jpthiery.arthena.domain.Environment;
import java.util.Optional;

public interface ConfigurationValueProvider {

  <T> Optional<T> getValue(ConfigurationKey key, Environment environment, Class<T> tClass);

  <T> Optional<T> getValue(ConfigurationKey key, Class<T> tClass);

  <T> void subscribeToValueChange(
      ConfigurationKey key, Environment environment, ValueChangeListener listener, Class<T> tClass);

  <T> void subscribeToValueChange(
      ConfigurationKey key, ValueChangeListener listener, Class<T> tClass);
}
