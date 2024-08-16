package com.github.jpthiery.arthena;

import com.github.jpthiery.arthena.domain.ConfigurationEntry;
import com.github.jpthiery.arthena.domain.ConfigurationKey;

public interface ValueChangeListener {

    <T> void valueChange(ConfigurationKey key, ConfigurationEntry<T> previous, ConfigurationEntry<T> current);

}
