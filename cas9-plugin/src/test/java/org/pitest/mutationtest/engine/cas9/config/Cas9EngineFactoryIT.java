package org.pitest.mutationtest.engine.cas9.config;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.nCopies;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.pitest.mutationtest.EngineArguments.arguments;
import static org.pitest.mutationtest.build.intercept.ast.ClassAstSettingsInterceptorFactory.FEATURE_NAME;
import static org.pitest.mutationtest.engine.gregor.config.Mutator.aor;
import static org.pitest.mutationtest.engine.gregor.config.Mutator.ror;
import static org.pitest.mutationtest.engine.gregor.config.Mutator.uoi;
import static org.pitest.plugin.ToggleStatus.ACTIVATE;

import java.io.File;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.classpath.ClassloaderByteArraySource;
import org.pitest.functional.FCollection;
import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.mutationtest.build.intercept.ast.ClassAstSettingsInterceptorFactory;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.mutationtest.engine.MethodName;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.plugin.FeatureSetting;

class Cas9EngineFactoryIT {

  private static final String SOURCE_DIRS_PROPERTY = "cas9-test.sources";

  private ClassByteArraySource byteSource;

  @BeforeEach
  void setUpAstSource() {
    byteSource = new ClassloaderByteArraySource(Cas9EngineTarget.class.getClassLoader());

    val sourceDir = System.getProperty(SOURCE_DIRS_PROPERTY, "src/test/java");
    val options = new ReportOptions();
    options.setSourceDirs(singleton(new File(sourceDir)));
    options.setClassPathElements(singleton("target/classes"));

    val feature = new FeatureSetting(FEATURE_NAME, ACTIVATE, emptyMap());
    val params = new InterceptorParameters(feature, options, byteSource);

    new ClassAstSettingsInterceptorFactory().createInterceptor(params);
  }

  @Test
  void createEngineShouldCreateMutationsWithGivenArguments() {
    // Arrange
    val arguments = arguments()
        .withExcludedMethods(singleton("skipIt"))
        .withMutators(asList("ROR", "AOR"));
    val expectedMutators = FCollection.map(aor(), MethodMutatorFactory::getGloballyUniqueId);
    val expectedMethod = MethodName.fromString("doIt");
    val expectedLine = Cas9EngineTarget.AOR_LINE;

    // Act
    val actual = new Cas9EngineFactory()
        .createEngine(arguments)
        .createMutator(byteSource)
        .findMutations(ClassName.fromClass(Cas9EngineTarget.class));

    // Assert
    assertAll(
        () -> assertThat(actual, hasSize(expectedMutators.size())),
        () -> assertThat(actual, everyItem(hasProperty("mutator", isIn(expectedMutators)))),
        () -> assertThat(actual, everyItem(hasProperty("method", is(expectedMethod)))),
        () -> assertThat(actual, everyItem(hasProperty("lineNumber", is(expectedLine))))
    );
  }

  @Test
  void createEngineShouldCreateMutationsWithDefaultArguments() {
    // Arrange
    val uoiSize = uoi().size();
    val aorSize = aor().size();
    val rorSize = ror().size();
    val expected = Stream.of(
        nCopies(uoiSize, "UOI"), // double y = ++a * x;
        nCopies(uoiSize, "UOI"), // double y = a * ++x;
        nCopies(aorSize, "AOR"), // double y = a / x;
        nCopies(uoiSize, "UOI"), // skipIt(++x, y)
        nCopies(uoiSize, "UOI"), // skipIt(x, ++y)
        singleton("VoidMethodCall"), // remove: skipIt(x, y)
        singleton("SBR"), // remove: if (a < b)
        nCopies(uoiSize, "UOI"), // if (++a < b)
        nCopies(uoiSize, "UOI"), // if (a < ++b)
        nCopies(rorSize, "ROR"), // if (a <= b)
        nCopies(uoiSize, "UOI"), // System.out.println("a: " + a++);
        singleton("VoidMethodCall"))  // System.out.println("a: " + a++);
        .flatMap(Collection::stream)
        .toArray(String[]::new);
    val operatorPattern = Pattern.compile("^(?:\\w+\\.)+([A-Za-z]+)\\d?Mutator$");

    // Act
    val actual = new Cas9EngineFactory()
        .createEngine(arguments())
        .createMutator(byteSource)
        .findMutations(ClassName.fromClass(Cas9EngineTarget.class))
        .stream()
        .map(details -> operatorPattern.matcher(details.getMutator()).replaceFirst("$1"))
        .collect(toList());

    // Assert
    assertThat(actual, containsInAnyOrder(expected));
  }
}
