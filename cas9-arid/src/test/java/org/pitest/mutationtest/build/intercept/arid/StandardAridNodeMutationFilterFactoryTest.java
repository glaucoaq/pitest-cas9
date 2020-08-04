package org.pitest.mutationtest.build.intercept.arid;

import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.File;
import java.util.Collection;
import java.util.stream.Stream;
import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pitest.classinfo.ClassName;
import org.pitest.classpath.ClassPathByteArraySource;
import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.mutationtest.build.intercept.arid.StandardAridMutationTargets.HasAridAndRelevantNodes;
import org.pitest.mutationtest.build.intercept.arid.StandardAridMutationTargets.HasMixedCompoundNodes;
import org.pitest.mutationtest.build.intercept.arid.StandardAridMutationTargets.HasOnlyAridNodes;
import org.pitest.mutationtest.build.intercept.arid.StandardAridMutationTargets.HasOnlyRelevantNodes;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.mutationtest.engine.Location;
import org.pitest.mutationtest.engine.MethodName;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;

class StandardAridNodeMutationFilterFactoryTest {

  private static final String SOURCE_DIR_PROPERTY = "cas9-test.sources";

  private static final String DEFAULT_SOURCE_DIR = "src/test/java";

  public static final String MUTATION_TARGET_FILE = "StandardAridMutationTargets.java";

  static Stream<Arguments> getFixture() {
    return Stream.of(
        arguments(HasOnlyAridNodes.class.getSimpleName(),
            asList(50, 54, 58, 62, 66, 67, 68),
            emptyList()),
        arguments(HasOnlyRelevantNodes.class.getSimpleName(),
            asList(80, 81, 85, 86, 87),
            asList(80, 81, 85, 86, 87)),
        arguments(HasAridAndRelevantNodes.class.getSimpleName(),
            asList(18, 19, 23, 27, 28, 29, 34, 35, 36),
            asList(18, 19, 27, 28, 29, 34, 36)),
        arguments(HasMixedCompoundNodes.class.getSimpleName(),
            asList(96, 97, 99, 100, 101),
            asList(99, 101)));
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("getFixture")
  void createdInterceptorShouldFilterMutationsUsingDefaultVoters(final String targetName,
      final Collection<Integer> mutatedLines, final Collection<Integer> expected) {
    // Arrange
    val options = new ReportOptions();
    val sourceDir = new File(getProperty(SOURCE_DIR_PROPERTY, DEFAULT_SOURCE_DIR));
    val byteSource = new ClassPathByteArraySource();
    val params = new InterceptorParameters(null, options, byteSource);
    val mutations = mutatedLines.stream()
        .map(line -> createMutation(targetName, line))
        .collect(toList());

    options.setFeatures(emptySet());
    options.setSourceDirs(singleton(sourceDir));
    // Act
    val actual = new StandardAridNodeMutationFilterFactory().createInterceptor(params)
        .intercept(mutations, null)
        .stream()
        .map(MutationDetails::getLineNumber)
        .collect(toList());
    // Assert
    assertThat(actual, is(expected));
  }

  private static MutationDetails createMutation(String className, Integer lineNumber) {
    val targetName = StandardAridMutationTargets.class.getName() + "$" + className;
    val location = new Location(ClassName.fromString(targetName), MethodName.fromString("any"), "V();");
    val id = new MutationIdentifier(location, lineNumber, "MUTATOR_NAME");
    return new MutationDetails(id, MUTATION_TARGET_FILE, "L" + lineNumber, lineNumber, 1);
  }
}
