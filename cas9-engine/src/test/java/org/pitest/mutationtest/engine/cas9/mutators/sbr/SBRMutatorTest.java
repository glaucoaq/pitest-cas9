package org.pitest.mutationtest.engine.cas9.mutators.sbr;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.pitest.mutationtest.testing.mutators.MutantMatcher.mutatesTo;

import com.google.gson.Gson;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.testing.ast.ClassAstSourceExtension;

@ExtendWith(ClassAstSourceExtension.class)
class SBRMutatorTest {

  static final String TARGET_MUTATIONS = "/sbr/SBRMutationTarget.json";

  private ClassAstSource astSource;

  @BeforeEach
  void setUp(ClassAstSource astSource) {
    this.astSource = astSource;
  }

  @Test
  void shouldFindMutationsForSbrOperator() {
    // Arrange
    val byteSource = new ClassPathByteArraySource();
    val className = ClassName.fromClass(SBRMutationTarget.class);
    val mutators = singleton((MethodMutatorFactory) SBRMutator.SBR_MUTATOR);
    // Act
    val actual = new AstGregorMutater(any -> true, byteSource, mutators, astSource)
        .findMutations(className)
        .stream()
        .map(MutationDetails::getLineNumber)
        .collect(toList());
    // Assert
    assertThat(actual, contains(11, 14, 16, 17));
  }

  static Stream<Arguments> getMutationFixture() {
    val details = loadTargetDetails()
        .map(MutationDetails::getId)
        .collect(toCollection(LinkedList::new));
    assert details.size() == 4;
    // removing blocks with nested blocks is not supported right now
    details.remove(2);
    return Stream.of(
        Arguments.of(details.pop(), "block1"),
        Arguments.of(details.pop(), "block2"),
        Arguments.of(details.pop(), "block4")
    );
  }

  @ParameterizedTest(name = "{index}: remove {1}")
  @MethodSource("getMutationFixture")
  void shouldGetMutationInClassFromMutatorId(MutationIdentifier mutationId, String folder) throws Exception {
    // Arrange
    val byteSource = new ClassPathByteArraySource();
    val mutators = singleton((MethodMutatorFactory) SBRMutator.SBR_MUTATOR);
    val expectedUrl = SBRMutatorTest.class.getResource("/sbr/" + folder + "/SBRMutationTarget.java");
    val expectedFile = Paths.get(expectedUrl.toURI());
    // Act
    val actual = new AstGregorMutater(any -> true, byteSource, mutators, astSource)
        .getMutation(mutationId);
    // Assert
    assertThat(actual, mutatesTo(expectedFile));
  }

  @Test
  void getMutationSadlyDoesNotSupportRemovingBlocksWithChildren() throws Exception  {
    // Arrange
    val byteSource = new ClassPathByteArraySource();
    val mutators = singleton((MethodMutatorFactory) SBRMutator.SBR_MUTATOR);
    val mutationId = loadTargetDetails()
        .skip(2)
        .findFirst()
        .map(MutationDetails::getId)
        .orElseThrow(AssertionError::new);
    val mutater = new AstGregorMutater(any -> true, byteSource, mutators, astSource);
    // Act & Assert
    assertThrows(NullPointerException.class, () -> mutater.getMutation(mutationId));
  }

  @SneakyThrows
  private static Stream<MutationDetails> loadTargetDetails() {
    val targetMutationsPath = Paths.get(SBRMutatorTest.class.getResource(TARGET_MUTATIONS).toURI());
    try (Reader reader = new FileReader(targetMutationsPath.toFile())) {
      return Stream.of(new Gson().fromJson(reader, MutationDetails[].class));
    }
  }
}
