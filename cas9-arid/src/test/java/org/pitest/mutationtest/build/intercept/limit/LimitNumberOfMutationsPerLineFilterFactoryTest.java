package org.pitest.mutationtest.build.intercept.limit;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.pitest.mutationtest.build.intercept.limit.LimitNumberOfMutationsPerLineFilterFactory.FEATURE_NAME;
import static org.pitest.mutationtest.build.intercept.limit.LimitNumberOfMutationsPerLineFilterFactory.LIMIT_PARAM;
import static org.pitest.mutationtest.testing.MutationTestUtils.getLineNumbers;
import static org.pitest.mutationtest.testing.MutationTestUtils.getOperators;
import static org.pitest.mutationtest.testing.MutationTestUtils.mutations;
import static org.pitest.plugin.ToggleStatus.ACTIVATE;

import java.util.Collection;
import java.util.stream.Stream;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.plugin.FeatureSetting;

class LimitNumberOfMutationsPerLineFilterFactoryTest {

  @Test
  void shouldFilterToSingleMutationPerLine() {
    // Arrange
    val conf = new FeatureSetting(FEATURE_NAME, ACTIVATE, emptyMap());
    val params = new InterceptorParameters(conf, null, null);
    val mutations = Stream.of(
        mutations(1, "ROR", "UOI", "AOR"),
        mutations(2, "AOR", "UOI"),
        mutations(3, "UOI"))
        .flatMap(identity())
        .collect(toList());
    // Act
    val actual = new LimitNumberOfMutationsPerLineFilterFactory()
        .createInterceptor(params)
        .intercept(mutations, null);
    // Assert
    assertAll(
        () -> assertThat(getOperators(actual), contains("ROR", "AOR", "UOI")),
        () -> assertThat(getLineNumbers(actual), contains(1, 2, 3))
    );
  }

  @Test
  void shouldFilterUpToThreeMutationsPerLine() {
    // Arrange
    val conf = new FeatureSetting(FEATURE_NAME, ACTIVATE,
        singletonMap(LIMIT_PARAM.name(), singletonList("3")));
    val params = new InterceptorParameters(conf, null, null);
    val mutations = Stream.of(
        mutations(1, "UOI", "AOR", "AOR", "UOI", "ROR"),
        mutations(2, "ROR", "ROR", "ROR", "ROR", "UOI", "UOI"),
        mutations(3, "UOI", "MATH"),
        mutations(4, "ROR"))
        .flatMap(identity())
        .collect(toList());
    val expected = Stream.of(
        asList("ROR", "AOR", "AOR"),
        asList("ROR", "ROR", "ROR"),
        asList("UOI", "Mat"),
        singletonList("ROR"))
        .flatMap(Collection::stream)
        .toArray(String[]::new);
    // Act
    val actual = new LimitNumberOfMutationsPerLineFilterFactory()
        .createInterceptor(params)
        .intercept(mutations, null);
    // Assert
    assertAll(
        () -> assertThat(getOperators(actual), contains(expected)),
        () -> assertThat(getLineNumbers(actual), contains(1, 1 , 1, 2, 2, 2, 3, 3, 4)));
  }
}
