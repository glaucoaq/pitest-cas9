package org.pitest.mutationtest.engine.cas9;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.pitest.mutationtest.engine.cas9.MutationDecompiler.CODE_PRINT_CONFIG;
import static org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator.CONDITIONALS_BOUNDARY_MUTATOR;
import static org.pitest.mutationtest.engine.gregor.mutators.IncrementsMutator.INCREMENTS_MUTATOR;
import static org.pitest.mutationtest.engine.gregor.mutators.InvertNegsMutator.INVERT_NEGS_MUTATOR;
import static org.pitest.mutationtest.engine.gregor.mutators.MathMutator.MATH_MUTATOR;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.google.gson.Gson;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pitest.classinfo.ClassName;
import org.pitest.classpath.ClassPathByteArraySource;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.config.Mutator;

@SuppressWarnings("unused")
class AstParserMutaterTest {

  static final Predicate<MethodInfo> ALL_METHODS = any -> true;

  static final Collection<MethodMutatorFactory> NEW_DEFAULTS = Mutator.newDefaults();

  static final Collection<MethodMutatorFactory> ALL_MUTATORS = Mutator.all();

  static final ClassName TARGET_CLASS_NAME = ClassName.fromClass(HasStatementsForAllNewDefaults.class);

  public static final String TARGET_MUTATIONS = "/mutator/HasStatementsForAllNewDefaults.json";

  static final MutationDecompiler DECOMPILER = MutationDecompiler.of(TARGET_CLASS_NAME);

  enum HasEnumConstructor {

    VALUE(0);

    int value;

    HasEnumConstructor(int value) {
      this.value = ++value;
    }
  }

  static class HasComparisonInAssertAndNegativeInReturn {

    int doIt(int i) {
      assert (i > 10);
      return -i;
    }
  }

  static class HasExpressionInLambda {

    void print(Collection<Integer> numbers) {
      numbers.stream().map(n -> n + 1).forEach(System.out::println);
    }
  }

  static Stream<Arguments> findMutationsFixture() {
    return Stream.of(
        Arguments.of(HasStatementsForAllNewDefaults.class, NEW_DEFAULTS, getMutatorIds(NEW_DEFAULTS)),
        Arguments.of(HasEnumConstructor.class, singleton(INCREMENTS_MUTATOR), getMutatorIds(INCREMENTS_MUTATOR)),
        Arguments.of(HasComparisonInAssertAndNegativeInReturn.class,
            asList(CONDITIONALS_BOUNDARY_MUTATOR, INVERT_NEGS_MUTATOR), getMutatorIds(INVERT_NEGS_MUTATOR)),
        Arguments.of(HasExpressionInLambda.class, singleton(MATH_MUTATOR), getMutatorIds(MATH_MUTATOR)));
  }

  @ParameterizedTest(name = "{index}: {2}")
  @MethodSource("findMutationsFixture")
  void shouldFindMutationsInClassFromMutatorsWithExpectedIds(Class<?> classToMutate,
      Collection<MethodMutatorFactory> mutators, Collection<String> expectedIds) {
    // Arrange
    val byteSource = new ClassPathByteArraySource();
    val className = ClassName.fromClass(classToMutate);
    val expected = expectedIds.toArray(new String[0]);
    // Act
    val actual = new AstParserMutater(ALL_METHODS, byteSource, mutators)
        .findMutations(className)
        .stream()
        .map(MutationDetails::getMutator)
        .map(mutator -> substringAfterLast(mutator, ".").replace("Mutator", ""))
        .collect(toSet());
    // Assert
    assertThat(actual, containsInAnyOrder(expected));
  }

  static class HasStatementSuitableForMathMutation {

    int sum(int a, int b) {
      return a + b;
    }
  }

  enum HasGeneratedCodeOnly { FOO, BAR }

  static class HasExpressionInAssert {

    void doIt(int i) {
      assert ((i + 20) > 10);
    }
  }

  static Stream<Arguments> doNotFindMutationsFixture() {
    return Stream.of(
        Arguments.of("HasStatementSuitableForMathMutation", emptySet()),
        Arguments.of("HasGeneratedCodeOnly", ALL_MUTATORS),
        Arguments.of("HasExpressionInAssert", asList(MATH_MUTATOR, CONDITIONALS_BOUNDARY_MUTATOR))
    );
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("doNotFindMutationsFixture")
  void shouldFindNoMutationsInClassFromMutators(String classToMutate, Collection<MethodMutatorFactory> mutators) {
    // Arrange
    val byteSource = new ClassPathByteArraySource();
    val className = ClassName.fromString(AstParserMutaterTest.class.getName() + "." + classToMutate);
    // Act
    val actual = new AstParserMutater(ALL_METHODS, byteSource, mutators)
        .findMutations(className);
    // Assert
    assertThat(actual, is(empty()));
  }

  static Stream<Arguments> getMutationFixture() {
    val details = loadTargetDetails()
        .map(MutationDetails::getId)
        .collect(toCollection(LinkedList::new));
    assert details.size() == 11;
    return Stream.of(
        Arguments.of(details.pop(), "i < 10", "i <= 10"),
        Arguments.of(details.pop(), "i < 10", "i >= 10"),
        Arguments.of(details.pop(), "i ^ j", "i & j"),
        Arguments.of(details.pop(), "i++", "i--"),
        Arguments.of(details.pop(), "doNothing();", "this;"),
        Arguments.of(details.pop(), "-j", "j"),
        Arguments.of(details.pop(), "return r", "return 0"),
        Arguments.of(details.pop(), "return b", "return true"),
        Arguments.of(details.pop(), "return b", "return false"),
        Arguments.of(details.pop(), "return a", "return \"\""),
        Arguments.of(details.pop(), "return o", "return null")
    );
  }

  @ParameterizedTest(name = "{index}: ({1}) => ({2})")
  @MethodSource("getMutationFixture")
  void shouldGetMutationInClassFromMutatorId(MutationIdentifier mutationId, String original, String mutated)
      throws Exception {
    // Arrange
    val byteSource = new ClassPathByteArraySource();
    val className = ClassName.fromClass(HasStatementsForAllNewDefaults.class);
    val expected = replaceCode(original, mutated);
    // Act
    val actual = new AstParserMutater(ALL_METHODS, byteSource, NEW_DEFAULTS)
        .getMutation(mutationId);
    // Assert
    assertThat(DECOMPILER.decompile(actual), is(expected));
  }

  private static Collection<String> getMutatorIds(MethodMutatorFactory... mutators) {
    return getMutatorIds(asList(mutators));
  }

  private static Collection<String> getMutatorIds(Collection<MethodMutatorFactory> mutators) {
    return mutators.stream()
        .map(MethodMutatorFactory::getGloballyUniqueId)
        .map(id -> substringAfterLast(id, ".").replace("Mutator", ""))
        .collect(toSet());
  }

  @SneakyThrows
  private static Stream<MutationDetails> loadTargetDetails() {
    val targetMutationsPath = Paths.get(AstParserMutaterTest.class.getResource(TARGET_MUTATIONS).toURI());
    try (Reader reader = new FileReader(targetMutationsPath.toFile())) {
      return Stream.of(new Gson().fromJson(reader, MutationDetails[].class));
    }
  }

  @SneakyThrows
  private static String replaceCode(String original, String mutated) {
    val targetSourceName = "/mutator/" + TARGET_CLASS_NAME.asInternalName() + ".java";
    val targetSourcePath = Paths.get(AstParserMutaterTest.class.getResource(targetSourceName).toURI());
    val modified = Files.readAllLines(targetSourcePath)
        .stream()
        .map(line -> line.replace(original, mutated))
        .collect(Collectors.joining(System.lineSeparator()));
    return StaticJavaParser.parse(modified)
        .getClassByName(TARGET_CLASS_NAME.getNameWithoutPackage().asJavaName())
        .map(type -> type.setAnnotations(NodeList.nodeList()))
        .map(node -> node.toString(CODE_PRINT_CONFIG))
        .orElse("");
  }
}
