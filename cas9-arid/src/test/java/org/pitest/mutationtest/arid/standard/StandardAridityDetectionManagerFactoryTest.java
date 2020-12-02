package org.pitest.mutationtest.arid.standard;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.pitest.functional.prelude.Prelude.not;

import com.github.javaparser.ast.Node;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.val;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pitest.ast.ClassAstSource;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.arid.standard.StandardAridMutationTargets.HasAridAndRelevantNodes;
import org.pitest.mutationtest.arid.standard.StandardAridMutationTargets.HasMixedCompoundNodes;
import org.pitest.mutationtest.arid.standard.StandardAridMutationTargets.HasOnlyAridNodes;
import org.pitest.mutationtest.arid.standard.StandardAridMutationTargets.HasOnlyRelevantNodes;
import org.pitest.mutationtest.engine.Location;
import org.pitest.mutationtest.engine.MethodName;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.testing.ast.ClassAstSourceExtension;

class StandardAridityDetectionManagerFactoryTest {

  public static final String MUTATION_TARGET_FILE = "StandardAridMutationTargets.java";

  private static final Function<MutationDetails, MutationIdentifier> DETAILS_TO_ID = MutationDetails::getId;

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
  @ExtendWith(ClassAstSourceExtension.class)
  void createdInterceptorShouldFilterMutationsUsingDefaultVoters(final String targetName,
      final Collection<Integer> mutatedLines, final Collection<Integer> expected, final ClassAstSource astSource) {
    // Arrange
    val mutations = mutatedLines.stream()
        .map(line -> createMutation(targetName, line))
        .collect(toList());
    Function<MutationIdentifier, MutationDetails> mutationById = mutations.stream()
        .collect(toMap(MutationDetails::getId, Function.identity()))
        ::get;
    Function<MutationIdentifier, Optional<Node>> mapper = mutationById
        .andThen(astSource::getAstNode);
    val manager = new StandardAridityDetectionManagerFactory().createManager(mapper);
    // Act
    val actual = mutations.stream()
        .filter(not(manager::decide))
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
