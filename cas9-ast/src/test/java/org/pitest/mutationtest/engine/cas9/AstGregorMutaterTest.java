package org.pitest.mutationtest.engine.cas9;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.pitest.mutationtest.engine.cas9.MutationDecompiler.CODE_PRINT_CONFIG;
import static org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator.CONDITIONALS_BOUNDARY_MUTATOR;
import static org.pitest.mutationtest.engine.gregor.mutators.IncrementsMutator.INCREMENTS_MUTATOR;
import static org.pitest.mutationtest.engine.gregor.mutators.InvertNegsMutator.INVERT_NEGS_MUTATOR;
import static org.pitest.mutationtest.engine.gregor.mutators.MathMutator.MATH_MUTATOR;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.pitest.classinfo.ClassName;
import org.pitest.classpath.ClassPathByteArraySource;
import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.mutationtest.build.intercept.ast.ClassAstSettingsInterceptorFactory;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.ClassInfo;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.mutationtest.engine.gregor.config.Mutator;
import org.pitest.reloc.asm.MethodVisitor;

@SuppressWarnings("unused")
class AstGregorMutaterTest {

  static final Predicate<MethodInfo> ALL_METHODS = any -> true;

  static final Collection<MethodMutatorFactory> NEW_DEFAULTS = Mutator.newDefaults();

  static final Collection<MethodMutatorFactory> ALL_MUTATORS = Mutator.all();

  static final ClassName TARGET_CLASS_NAME = ClassName.fromClass(HasStatementsForAllNewDefaults.class);

  public static final String TARGET_MUTATIONS = "/mutator/HasStatementsForAllNewDefaults.json";

  static final MutationDecompiler DECOMPILER = MutationDecompiler.of(TARGET_CLASS_NAME);

  private static final String BUILD_PROPERTIES_FILE = "/build.properties";

  private static final Properties BUILD_PROPERTIES = loadBuildProperties();

  private static final String SOURCE_DIRS_PROPERTY = "sourceDirectory";

  private static final String CLASSES_DIRS_PROPERTY = "outputDirectory";

  private static final String CLASSPATH_FILE = "/classpath.txt";

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
    val actual = new AstGregorMutater(ALL_METHODS, byteSource, mutators)
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
    val className = ClassName.fromString(AstGregorMutaterTest.class.getName() + "." + classToMutate);
    // Act
    val actual = new AstGregorMutater(ALL_METHODS, byteSource, mutators)
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
    val expected = replaceCode(original, mutated);
    // Act
    val actual = new AstGregorMutater(ALL_METHODS, byteSource, NEW_DEFAULTS)
        .getMutation(mutationId);
    // Assert
    assertThat(DECOMPILER.decompile(actual), is(expected));
  }

  @RequiredArgsConstructor
  static class AssertMethodAstInfoMutatorFactory implements AstSupportMutatorFactory {

    private final Consumer<MethodAstInfo> consume;

    @Override
    public MethodVisitor create(MutationContext context, MethodAstInfo astInfo, MethodInfo methodInfo,
        MethodVisitor visitor) {
      consume.accept(astInfo);
      return visitor;
    }

    @Override
    public String getGloballyUniqueId() {
      return AssertMethodAstInfoMutatorFactory.class.getName();
    }

    @Override
    public String getName() {
      return "AssertMethodAstInfoMutatorFactory";
    }
  }

  @Test
  void shouldNotCreateAstMethodVisitorInClassWhenSourceIsNotInitialized() {
    // Arrange
    val byteSource = new ClassPathByteArraySource();
    val mutator = spy(new AssertMethodAstInfoMutatorFactory(
        info -> fail("Should not call create without AST initialization")));
    val contextCaptor = ArgumentCaptor.forClass(MutationContext.class);
    val methodInfoCaptor = ArgumentCaptor.forClass(MethodInfo.class);
    // Act
    val actual = new AstGregorMutater(ALL_METHODS, byteSource, singleton(mutator))
        .findMutations(TARGET_CLASS_NAME);
    // Assert
    verify(mutator, atLeastOnce()).create(contextCaptor.capture(), methodInfoCaptor.capture(), any(MethodVisitor.class));
    val classNames = contextCaptor.getAllValues().stream()
        .map(MutationContext::getClassInfo)
        .map(ClassInfo::getName)
        .collect(toSet());
    val methodNames = methodInfoCaptor.getAllValues().stream()
        .map(MethodInfo::getName)
        .collect(toSet());
    assertAll(
        () -> assertThat(classNames, contains(TARGET_CLASS_NAME.asInternalName())),
        () -> assertThat(methodNames, containsInAnyOrder("<init>", "doIt", "testIt", "echoIt", "doNothing"))
    );
  }

  @Test
  void shouldCreateAstMethodVisitorInClassWhenSourceIsInitialized() throws Exception {
    // Arrange
    val byteSource = new ClassPathByteArraySource();
    val astInfoList = new ArrayList<MethodAstInfo>();
    val mutator = new AssertMethodAstInfoMutatorFactory(astInfoList::add);
    // Act
    enableAstParsing();
    val actual = new AstGregorMutater(ALL_METHODS, byteSource, singleton(mutator))
        .findMutations(TARGET_CLASS_NAME);
    disableAstParsing();
    // Assert
    val methodNames = astInfoList.stream()
        .map(MethodAstInfo::getMethodAst)
        .map(CallableDeclaration::getNameAsString)
        .collect(toSet());
    assertThat(methodNames, containsInAnyOrder("doIt", "testIt", "echoIt", "doNothing"));
  }

  static class MethodAstInfoIncrementsMutator implements AstSupportMutatorFactory {

    @Override
    public MethodVisitor create(MutationContext context, MethodAstInfo astInfo, MethodInfo methodInfo,
        MethodVisitor visitor) {
      return INCREMENTS_MUTATOR.create(context, methodInfo, visitor);
    }

    @Override
    public String getGloballyUniqueId() {
      return INCREMENTS_MUTATOR.getGloballyUniqueId();
    }

    @Override
    public String getName() {
      return INCREMENTS_MUTATOR.getName();
    }
  }

  @Test
  void shouldGetMutationAndAstInClassWhenSourceIsInitialized() throws Exception {
    // Arrange
    val byteSource = new ClassPathByteArraySource();
    val mutator = spy(new MethodAstInfoIncrementsMutator());
    val mutationId = loadTargetDetails()
        .map(MutationDetails::getId)
        .filter(id -> id.getMutator().equals(mutator.getGloballyUniqueId()))
        .findFirst()
        .orElseThrow(AssertionError::new);
    val astInfoCaptor = ArgumentCaptor.forClass(MethodAstInfo.class);
    // Act
    enableAstParsing();
    val actual = new AstGregorMutater(ALL_METHODS, byteSource, singleton(mutator))
        .getMutation(mutationId);
    disableAstParsing();
    // Assert
    verify(mutator, atLeastOnce())
        .create(any(MutationContext.class), astInfoCaptor.capture(), any(MethodInfo.class), any(MethodVisitor.class));
    val methodNames = astInfoCaptor.getAllValues().stream()
        .map(MethodAstInfo::getMethodAst)
        .map(CallableDeclaration::getNameAsString)
        .collect(toSet());
    assertAll(
        () -> assertThat(methodNames, containsInAnyOrder("doIt", "testIt", "echoIt", "doNothing")),
        () -> assertThat(DECOMPILER.decompile(actual), is(replaceCode("i++", "i--"))));
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
    val targetMutationsPath = Paths.get(AstGregorMutaterTest.class.getResource(TARGET_MUTATIONS).toURI());
    try (Reader reader = new FileReader(targetMutationsPath.toFile())) {
      return Stream.of(new Gson().fromJson(reader, MutationDetails[].class));
    }
  }

  @SneakyThrows
  private static String replaceCode(String original, String mutated) {
    val targetSourceName = "/mutator/" + TARGET_CLASS_NAME.asInternalName() + ".java";
    val targetSourcePath = Paths.get(AstGregorMutaterTest.class.getResource(targetSourceName).toURI());
    val modified = Files.readAllLines(targetSourcePath)
        .stream()
        .map(line -> line.replace(original, mutated))
        .collect(joining(System.lineSeparator()));
    return StaticJavaParser.parse(modified)
        .getClassByName(TARGET_CLASS_NAME.getNameWithoutPackage().asJavaName())
        .map(type -> type.setAnnotations(NodeList.nodeList()))
        .map(node -> node.toString(CODE_PRINT_CONFIG))
        .orElse("");
  }

  private static void enableAstParsing() throws IOException, URISyntaxException {
    val options = new ReportOptions();
    options.setSourceDirs(singleton(new File(BUILD_PROPERTIES.getProperty(SOURCE_DIRS_PROPERTY))));
    options.setClassPathElements(loadClassPathElements());
    val params = new InterceptorParameters(null, options, null);
    val settingsFactory = new ClassAstSettingsInterceptorFactory();
    settingsFactory.createInterceptor(params);
  }

  private static void disableAstParsing() {
    val params = new InterceptorParameters(null, null, null);
    val settingsFactory = new ClassAstSettingsInterceptorFactory();
    settingsFactory.createInterceptor(params);
  }

  @SneakyThrows
  private static Properties loadBuildProperties() {
    val buildProps = new Properties();
    try (InputStream is = AstGregorMutaterTest.class.getResourceAsStream(BUILD_PROPERTIES_FILE)) {
      buildProps.load(is);
    }
    return buildProps;
  }

  private static Collection<String> loadClassPathElements() throws IOException, URISyntaxException {
    val classPathFile = Paths.get(AstGregorMutaterTest.class.getResource(CLASSPATH_FILE).toURI());
    val dependencies = Files.readAllLines(classPathFile).stream()
        .flatMap(line -> Stream.of(line.split(":")))
        .collect(toSet());
    dependencies.add(BUILD_PROPERTIES.getProperty(CLASSES_DIRS_PROPERTY));
    return dependencies;
  }
}
