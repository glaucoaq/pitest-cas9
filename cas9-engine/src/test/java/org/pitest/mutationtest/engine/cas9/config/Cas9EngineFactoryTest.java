package org.pitest.mutationtest.engine.cas9.config;

import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.pitest.mutationtest.EngineArguments.arguments;
import static org.pitest.mutationtest.build.intercept.MutationFilterTestUtils.getLineNumbers;
import static org.pitest.mutationtest.build.intercept.MutationFilterTestUtils.getMethods;
import static org.pitest.mutationtest.build.intercept.MutationFilterTestUtils.getOperators;
import static org.pitest.mutationtest.engine.gregor.config.Mutator.aor;
import static org.pitest.mutationtest.engine.gregor.config.Mutator.ror;
import static org.pitest.mutationtest.engine.gregor.config.Mutator.uoi;

import java.util.Collection;
import java.util.stream.Stream;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.pitest.classinfo.ClassName;
import org.pitest.classpath.ClassPathByteArraySource;

class Cas9EngineFactoryTest {

  static class TargetExample {

    static final int AOR_LINE = 35;

    static final int ROR_LINE = AOR_LINE + 5;

    @SuppressWarnings("unused")
    void doIt(final int a) {
      double x = Math.random();
      double y = a * x; // AOR
      skipIt(x, y);
    }

    void skipIt(final double a, final double b) {
      if (a < b) { // ROR, skipped by method exclusion
        System.out.println("a: " + a);
      }
    }
  }

  @Test
  void shouldCreateEngineWithGivenArguments() {
    // Arrange
    val arguments = arguments()
        .withExcludedMethods(singleton("skipIt"))
        .withMutators(asList("ROR", "AOR"));
    val byteSource = new ClassPathByteArraySource();
    val className = ClassName.fromClass(TargetExample.class);
    val expectedSize = aor().size();
    val expectedOperator = nCopies(expectedSize, "AOR").toArray(new String[0]);
    val expectedMethod = nCopies(expectedSize, "doIt").toArray(new String[0]);
    val expectedLine = nCopies(expectedSize, TargetExample.AOR_LINE).toArray(new Integer[0]);
    // Act
    val actual = new Cas9EngineFactory()
        .createEngine(arguments)
        .createMutator(byteSource)
        .findMutations(className);
    // Assert
    assertAll(
        () -> assertThat(getOperators(actual), contains(expectedOperator)),
        () -> assertThat(getMethods(actual), contains(expectedMethod)),
        () -> assertThat(getLineNumbers(actual), contains(expectedLine)));
  }

  @Test
  void shouldCreateEngineWithDefaultArguments() {
    // Arrange
    val byteSource = new ClassPathByteArraySource();
    val className = ClassName.fromClass(TargetExample.class);
    val expected = Stream.of(
        nCopies(uoi().size(), "UOI"), // double y = ++a * x;
        nCopies(uoi().size(), "UOI"), // double y = a * ++x;
        nCopies(aor().size(), "AOR"), // double y = a / x;
        nCopies(uoi().size(), "UOI"), // skipIt(++x, y)
        nCopies(uoi().size(), "UOI"), // skipIt(x, ++y)
        singleton("Voi"), // remove: skipIt(x, y)
        nCopies(uoi().size(), "UOI"), // if (++a < b)
        nCopies(uoi().size(), "UOI"), // if (a < ++b)
        nCopies(ror().size(), "ROR"), // if (a <= b)
        nCopies(uoi().size(), "UOI"), // System.out.println("a: " + a++);
        singleton("Voi"))  // System.out.println("a: " + a++);
        .flatMap(Collection::stream)
        .toArray(String[]::new);

    // Act
    val actual = new Cas9EngineFactory()
        .createEngine(arguments())
        .createMutator(byteSource)
        .findMutations(className);
    // Assert
    assertThat(getOperators(actual), contains(expected));
  }
}
