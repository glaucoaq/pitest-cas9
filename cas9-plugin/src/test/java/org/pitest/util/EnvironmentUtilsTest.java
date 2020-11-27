package org.pitest.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class EnvironmentUtilsTest {

  private static final String ENV_VAR_NAME = "EnvironmentUtilsTest_value";

  private static final String ENV_VAR_VALUE = "anything";

  @Test
  void shouldSetEnvironmentVariable() throws Exception {
    assertNull(System.getenv(ENV_VAR_NAME));
    EnvironmentUtils.setenv(ENV_VAR_NAME, ENV_VAR_VALUE);
    assertEquals(ENV_VAR_VALUE, System.getenv(ENV_VAR_NAME));
  }
}
