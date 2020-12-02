package org.pitest.mutationtest.build.intercept.coverage;

import static java.util.Collections.emptyList;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.pitest.mutationtest.testing.MutationTestUtils.getLineNumbers;
import static org.pitest.mutationtest.testing.MutationTestUtils.getOperators;
import static org.pitest.mutationtest.testing.MutationTestUtils.mutations;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.pitest.coverage.CoverageDatabase;
import org.pitest.coverage.TestInfo;

class CoveredLinesOnlyFilterFactoryTest {

  private static final CoverageLookupTestPrioritiserFactory PRIORITISER_FACTORY =
      new CoverageLookupTestPrioritiserFactory();

  @Test
  void shouldFilterOutNoCoverageMutations() {
    // Arrange
    val mutations = Stream.of(
        mutations(1, "ROR"),
        mutations(2, "UOI"),
        mutations(3, "AOR"))
        .flatMap(identity())
        .collect(toList());
    val coverage = mock(CoverageDatabase.class);

    when(coverage.getTestsForClassLine(argThat(line -> line != null && line.getLineNumber() == 1)))
        .thenReturn(createTests(1));
    when(coverage.getTestsForClassLine(argThat(line -> line != null && line.getLineNumber() == 2)))
        .thenReturn(emptyList());
    when(coverage.getTestsForClassLine(argThat(line -> line != null && line.getLineNumber() == 3)))
        .thenReturn(createTests(3));
    // Act
    PRIORITISER_FACTORY.makeTestPrioritiser(null, null, coverage);
    val actual = new CoveredLinesOnlyFilterFactory()
        .createInterceptor(null)
        .intercept(mutations, null);
    PRIORITISER_FACTORY.makeTestPrioritiser(null, null, null);
    // Assert
    assertAll(
        () -> assertThat(getOperators(actual), contains("ROR", "AOR")),
        () -> assertThat(getLineNumbers(actual), contains(1, 3))
    );
  }

  @Test
  void shouldReturnEmptyIfCoverageLookupIsNotInitialized() {
    // Arrange
    val mutations = Stream.of(
        mutations(1, "ROR"),
        mutations(2, "UOI"),
        mutations(3, "AOR"))
        .flatMap(identity())
        .collect(toList());
    // Act
    val actual = new CoveredLinesOnlyFilterFactory()
        .createInterceptor(null)
        .intercept(mutations, null);
    // Assert
    assertThat(actual, is(empty()));
  }

  private static Collection<TestInfo> createTests(int count) {
    return IntStream.range(0, count)
        .mapToObj(index -> new TestInfo(null, null, index, Optional.empty(), 0))
        .collect(toList());
  }
}
