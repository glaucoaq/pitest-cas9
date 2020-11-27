package org.pitest.mutationtest.arid.expert;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.pitest.mutationtest.arid.expert.ExpertAridMutationTarget.MUTATED_LINE;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.EmptyStmt;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.arid.AridityDetectionVoter;
import org.pitest.mutationtest.arid.NodeAridity;
import org.pitest.mutationtest.engine.Location;
import org.pitest.mutationtest.engine.MethodName;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;

class ExpertAridityDetectionManagerFactoryTest {

  private static final Class<?> MUTATION_TARGET_CLASS = ExpertAridMutationTarget.class;

  private static final String MUTATION_TARGET_FILE = "ExpertAridMutationTarget.java";

  private static final Function<MutationIdentifier, Optional<Node>> MUTATION_NODE_MAPPER =
      id -> Optional.of(new EmptyStmt());

  static Stream<Arguments> getModeAndRemoveFixture() {
    return Stream.of(
        arguments(AridityDetectionMode.AFFIRMATIVE, true),
        arguments(AridityDetectionMode.CONSENSUS, false),
        arguments(AridityDetectionMode.UNANIMOUS, false));
  }

  @ParameterizedTest
  @MethodSource("getModeAndRemoveFixture")
  void createdInterceptorShouldFilterMutationUsingMode(final AridityDetectionMode mode, final boolean removeMutation) {
    // Arrange
    val loader = ExpertAridityDetectionManagerFactoryTest.class.getClassLoader();
    val mutation = new MutationDetails(new MutationIdentifier(new Location(
        ClassName.fromClass(MUTATION_TARGET_CLASS), MethodName.fromString("any"), "V()"), 0, "MUTATOR_ID"),
        MUTATION_TARGET_FILE, "Single mutation", MUTATED_LINE, 0);
    // Act
    val actual = new ExpertAridityDetectionManagerFactory(loader, mode)
        .createManager(MUTATION_NODE_MAPPER)
        .decide(mutation);
    // Assert
    assertThat(actual, is(removeMutation));
  }

  public static class AlwaysAbstainVoter implements AridityDetectionVoter {

    @Override
    public NodeAridity vote(Node node) {
      return NodeAridity.ABSTAIN;
    }
  }

  public static class AlwaysAridVoter implements AridityDetectionVoter {

    @Override
    public NodeAridity vote(Node node) {
      return NodeAridity.ARID;
    }
  }

  public static class AlwaysRelevantVoter implements AridityDetectionVoter {

    @Override
    public NodeAridity vote(Node node) {
      return NodeAridity.RELEVANT;
    }
  }
}
