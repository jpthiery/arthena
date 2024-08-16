package com.github.jpthiery.arthena.zookeeper;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ZnodePathTest {

  @ParameterizedTest
  @ValueSource(
      strings = {"/a", "/a/b", "/1", "/1/2", "/a/1", "/1/a", "/arthena/my/app/props/drive1"})
  public void itShouldBeValidZnodePath(String input) {
    new ZnodePath(input);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"//a", "/a//b", "a", "1", "/&", "/é", "/the.props", "/the-props", "/a/b/", "/a/"})
  public void itShouldBeInvalidZnodePath(String input) {
    Assertions.assertThatThrownBy(
            () -> {
              new ZnodePath(input);
            })
        .isInstanceOf(IllegalArgumentException.class);
  }
}
