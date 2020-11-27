package org.pitest.mutationtest.arid.ranges;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.pitest.mutationtest.arid.ranges.LineRangeConfigReader.ARID_RANGE_PROPERTY;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pitest.classpath.ClassPath;
import org.pitest.mutationtest.arid.AridityDetectionManager;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.testing.MutationTestUtils;

class LineRangeAridityDetectionManagerFactoryTest {

  private AridityDetectionManager manager;

  private ClassPath classPath;

  private Path configFile;

  @BeforeEach
  void setUp(@TempDir Path temp) throws Exception {
    classPath = mock(ClassPath.class);
    manager = LineRangeAridityDetectionManagerFactory.of(temp, classPath)
        .createManager(id -> Optional.empty());
    configFile = temp.resolve(MutationTestUtils.TEST_CLASS_NAME.asJavaName() + ".properties");

    when(classPath.findResource(anyString())).thenReturn(configFile.toUri().toURL());
  }

  static Stream<Arguments> decideAridMutationsFixture() {
    return Stream.of(
        arguments("1-1", 1, true),
        arguments("1-1", 2, false),
        arguments("1-2", 1, true),
        arguments("1-2", 2, true),
        arguments("1-2", 3, false),
        arguments("1-3", 2, true),
        arguments("1-1,2-4", 3, true),
        arguments("1-1,2-4", 5, false),
        arguments("1-4,5-5,7-10", 8, true),
        arguments("1-4,5-5,7-7", 8, false));
  }

  @ParameterizedTest(name = "{index}: [{0}] {1} -> {2}")
  @MethodSource("decideAridMutationsFixture")
  void shouldDecideAridMutationsWhenInLineRange(String range, Integer lineNumber, boolean expected)
      throws Exception {
    // Arrange
    val details = mutationWithLine(lineNumber);
    Files.write(configFile, configLine(range));
    // Act
    val actual = manager.decide(details);
    // Assert
    assertThat(actual, is(expected));
  }

  @Test
  void shouldDecideFalseIfConfigFileDoesNotExist() {
    // Arrange
    val details = mutationWithLine(1);
    when(classPath.findResource(anyString())).thenReturn(null);
    // Act
    val actual = manager.decide(details);
    // Assert
    assertThat(actual, is(false));
  }

  @Test
  void shouldThrowIfHasInvalidRangeValues() throws Exception {
    // Arrange
    val details = mutationWithLine(1);
    Files.write(configFile, configLine("x-b"));
    // Act
    assertThrows(NumberFormatException.class, () -> manager.decide(details));
  }

  private static MutationDetails mutationWithLine(int lineNumber) {
    return MutationTestUtils.mutations(lineNumber, "UOI")
        .findFirst()
        .orElseThrow(IllegalStateException::new);
  }

  private static Collection<String> configLine(String range) {
    return Collections.singleton(ARID_RANGE_PROPERTY + "=" + range);
  }
}
