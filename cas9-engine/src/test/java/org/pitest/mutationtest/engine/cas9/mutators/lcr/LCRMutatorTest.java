package org.pitest.mutationtest.engine.cas9.mutators.lcr;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.pitest.mutationtest.engine.cas9.mutators.lcr.LCRMutator.lcr;
import static org.pitest.mutationtest.testing.mutators.MutantMatcher.mutatesTo;
import static org.pitest.mutationtest.testing.mutators.MutantMatcher.replaces;

import com.google.gson.Gson;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pitest.ast.ClassAstSource;
import org.pitest.classinfo.ClassName;
import org.pitest.classpath.ClassPathByteArraySource;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.cas9.AstGregorMutater;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.testing.ast.ClassAstSourceExtension;

@ExtendWith(ClassAstSourceExtension.class)
class LCRMutatorTest {

  static final String TARGET_MUTATIONS = "/lcr/LCRMutationTarget.json";

  static final String TARGET_MUTATIONS_IFSTMT = "/lcr/LCRMutationTarget-ifstmt.json";

  private static final Predicate<MethodInfo> DO_IT_FILTER = info -> info.getName().equals("doIt");

  private static final Predicate<MethodInfo> TEST_IT_FILTER = info -> info.getName().equals("testIt");

  private ClassAstSource astSource;

  @BeforeEach
  void setUp(ClassAstSource astSource) {
    this.astSource = astSource;
  }

  static Stream<Arguments> findMutationFixture() {
    return Stream.of(
        Arguments.of(DO_IT_FILTER, Arrays.asList(9, 9, 9, 9, 10, 10, 11, 11, 12, 12, 12, 12, 12, 12)),
        Arguments.of(TEST_IT_FILTER, Arrays.asList(18, 18, 18, 18, 19, 19, 19, 19)));
  }

  @ParameterizedTest
  @MethodSource("findMutationFixture")
  void shouldFindMutationsForLcrOperator(Predicate<MethodInfo> filter, Collection<Integer> items) {
    // Arrange
    val byteSource = new ClassPathByteArraySource();
    val className = ClassName.fromClass(LCRMutationTarget.class);
    val expected = items.toArray(new Integer[0]);
    // Act
    val actual = new AstGregorMutater(filter, byteSource, lcr(), astSource)
        .findMutations(className)
        .stream()
        .map(MutationDetails::getLineNumber)
        .collect(toList());
    // Assert
    assertThat(actual, contains(expected));
  }

  static Stream<Arguments> getMutationFixture() {
    val details = loadTargetDetails(TARGET_MUTATIONS)
        .map(MutationDetails::getId)
        .collect(toCollection(LinkedList::new));
    assert details.size() == 14;
    details.remove(11);
    details.remove(10);
    return Stream.of(
        Arguments.of(details.pop(), "(a && b)", "b"),
        Arguments.of(details.pop(), "(a && b)", "false"),
        Arguments.of(details.pop(), "(a && b)", "a"),
        Arguments.of(details.pop(), "boolean t = (a && b);", "if (a) ; boolean t = false;"),
        Arguments.of(details.pop(), "(x > y && b)", "(x > y)"),
        Arguments.of(details.pop(), "boolean u = (x > y && b);", "if (x > y) ; boolean u = false;"),
        Arguments.of(details.pop(), "(a && y == x)", "(y == x)"),
        Arguments.of(details.pop(), "(a && y == x)", "false"),
        Arguments.of(details.pop(), "(t && u && v)", "(u && v)"),
        Arguments.of(details.pop(), "(t && u && v)", "false"),
        Arguments.of(details.pop(), "(t && u && v)", "(t && u)"),
        Arguments.of(details.pop(), "boolean r = (t && u && v);", "if (t && u) ; boolean r = false;")
    );
  }

  @ParameterizedTest(name = "{index}: {1} => {2}")
  @MethodSource("getMutationFixture")
  void shouldGetMutationInClassFromMutatorId(MutationIdentifier mutationId, String original, String mutation) {
    // Arrange
    val byteSource = new ClassPathByteArraySource();
    // Act
    val actual = new AstGregorMutater(DO_IT_FILTER, byteSource, lcr(), astSource)
        .getMutation(mutationId);
    // Assert
    assertThat(actual, replaces(original, mutation));
  }

  static Stream<Arguments> getMutationFixtureIfStmt() {
    val details = loadTargetDetails(TARGET_MUTATIONS_IFSTMT)
        .map(MutationDetails::getId)
        .collect(toCollection(LinkedList::new));
    assert details.size() == 8;
    details.pop();
    details.remove(4);
    details.remove(3);
    return Stream.of(
        Arguments.of(details.pop(), "test1"),
        Arguments.of(details.pop(), "test2"),
        Arguments.of(details.pop(), "test3"),
        Arguments.of(details.pop(), "test4")
    );
  }

  @ParameterizedTest(name = "{index}: {1}")
  @MethodSource("getMutationFixtureIfStmt")
  void shouldGetMutationInClassFromMutatorId2(MutationIdentifier mutationId, String folder) throws Exception {
    // Arrange
    val byteSource = new ClassPathByteArraySource();
    val expectedUrl = LCRMutatorTest.class.getResource("/lcr/" + folder + "/LCRMutationTarget.java");
    val expectedFile = Paths.get(expectedUrl.toURI());
    // Act
    val actual = new AstGregorMutater(TEST_IT_FILTER, byteSource, lcr(), astSource)
        .getMutation(mutationId);
    // Assert
    assertThat(actual, mutatesTo(expectedFile));
  }

  @SneakyThrows
  private static Stream<MutationDetails> loadTargetDetails(String name) {
    val targetMutationsPath = Paths.get(LCRMutatorTest.class.getResource(name).toURI());
    try (Reader reader = new FileReader(targetMutationsPath.toFile())) {
      return Stream.of(new Gson().fromJson(reader, MutationDetails[].class));
    }
  }
}
