package org.pitest.util;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;

/**
 * Ugly hack for sharing data between the main process and the forked minion process.
 *
 * See https://dzone.com/articles/how-to-change-environment-variables-in-java
 */
@UtilityClass
public class EnvironmentUtils {

  private static final String LIST_ELEMENTS_SEPARATOR = ",";

  public String getenv(String key, String defaultValue) {
    val value = System.getenv(key);
    return value != null ? value : defaultValue;
  }

  public <T> Collection<T> getenv(String key, Function<String, T> mapper, Collection<T> defaultValue) {
    val value = System.getenv(key);
    return value == null
        ? defaultValue
        : Stream.of(value.split(LIST_ELEMENTS_SEPARATOR))
            .map(mapper)
            .collect(toList());
  }

  @SneakyThrows
  public void setenv(String key, String value) {
    val processEnvironment = Class.forName("java.lang.ProcessEnvironment");
    val unmodifiableMapField = getAccessibleField(processEnvironment, "theUnmodifiableEnvironment");
    val unmodifiableMap = unmodifiableMapField.get(null);
    injectIntoUnmodifiableMap(key, value, unmodifiableMap);
  }

  public void setenv(String key, Collection<?> values) {
    val value = values.stream()
        .map(Object::toString)
        .collect(joining(LIST_ELEMENTS_SEPARATOR));
    setenv(key, value);
  }

  private Field getAccessibleField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    val field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    return field;
  }

  private void injectIntoUnmodifiableMap(String key, String value, Object map) throws ReflectiveOperationException {
    val unmodifiableMap = Class.forName("java.util.Collections$UnmodifiableMap");
    val field = getAccessibleField(unmodifiableMap, "m");
    @SuppressWarnings("unchecked")
    val environment = (Map<String, String>) field.get(map);
    environment.put(key, value);
  }
}
