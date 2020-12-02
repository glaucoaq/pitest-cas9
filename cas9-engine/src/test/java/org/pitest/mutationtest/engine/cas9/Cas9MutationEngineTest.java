package org.pitest.mutationtest.engine.cas9;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Optional;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pitest.ast.ClassAstSource;
import org.pitest.ast.ClassAstSourceContext;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.mutationtest.engine.MutationEngine;
import org.pitest.mutationtest.engine.cas9.config.Cas9EngineConfiguration;
import org.pitest.mutationtest.engine.cas9.config.Cas9Mutater;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;

class Cas9MutationEngineTest {

  private static final Collection<String> OPERATORS =
      unmodifiableCollection(asList("OP1", "OP2", "OP3"));

  private static final ClassAstSource CLASS_AST_SOURCE = (className, fileName) -> Optional.empty();

  private MutationEngine engine;

  @BeforeEach
  void setUp() {
    val astContext = mock(ClassAstSourceContext.class);
    when(astContext.getAstSource()).thenReturn(Optional.of(CLASS_AST_SOURCE));

    val mutators= OPERATORS.stream()
        .map(operator -> {
          MethodMutatorFactory mutator = mock(MethodMutatorFactory.class);
          when(mutator.getGloballyUniqueId()).thenReturn(operator);
          when(mutator.getName()).thenReturn(operator);
          return mutator;
        })
        .collect(toList());

    val config = mock(Cas9EngineConfiguration.class);
    when(config.mutators()).thenAnswer(invocation -> mutators);
    when(config.methodFilter()).thenReturn(info -> true);
    when(config.astContext()).thenReturn(astContext);

    engine = Cas9MutationEngine.withConfig(config);
  }

  @Test
  void getMutatorNamesShouldReturnExpectedNames() {
    // Arrange
    String[] expected = OPERATORS.toArray(new String[0]);

    // Act
    val actual = engine.getMutatorNames();

    // Assert
    assertThat(actual, containsInAnyOrder(expected));
  }

  @Test
  void createMutatorShouldCreateCas9MutaterWithAstSource() {
    // Arrange
    val byteSource = mock(ClassByteArraySource.class);
    when(byteSource.getBytes(any())).thenReturn(Optional.empty());

    // Act
    val actual = engine.createMutator(byteSource);

    // Assert
    val astSource = actual instanceof Cas9Mutater
        ? ((Cas9Mutater) actual).getClassAstSource()
        : null;
    assertAll(
        () -> assertThat(actual, instanceOf(Cas9Mutater.class)),
        () -> assertThat(astSource, sameInstance(CLASS_AST_SOURCE))
    );
  }
}
