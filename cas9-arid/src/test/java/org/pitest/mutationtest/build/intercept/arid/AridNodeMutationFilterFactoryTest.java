package org.pitest.mutationtest.build.intercept.arid;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.pitest.mutationtest.build.intercept.arid.LineRangeAridNodeMutationFilterFactory.FEATURE_NAME;
import static org.pitest.mutationtest.build.intercept.arid.LineRangeAridNodeMutationFilterFactory.ROOT_PARAM;
import static org.pitest.mutationtest.testing.MutationTestUtils.mutations;
import static org.pitest.plugin.ToggleStatus.ACTIVATE;

import com.google.common.collect.Sets;
import java.util.function.Function;
import java.util.stream.IntStream;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pitest.classpath.ClassPath;
import org.pitest.mutationtest.arid.AridityDetectionManager;
import org.pitest.mutationtest.arid.AridityDetectionManagerFactory;
import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.mutationtest.build.MutationInterceptorFactory;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.plugin.FeatureSetting;

class AridNodeMutationFilterFactoryTest {

  private InterceptorParameters params;

  private AridityDetectionManager manager;

  private AridityDetectionManagerFactory factory;

  @BeforeEach
  void setUp() {
    val conf = new FeatureSetting(FEATURE_NAME, ACTIVATE, singletonMap(ROOT_PARAM.name(), singletonList("cas9-line")));
    val options = mock(ReportOptions.class);
    val classPath = mock(ClassPath.class);

    factory = mock(AridityDetectionManagerFactory.class);
    manager = mock(AridityDetectionManager.class);

    when(options.getClassPath()).thenReturn(classPath);
    when(factory.createManager(any())).thenReturn(manager);

    params = new InterceptorParameters(conf, options, null);
  }

  @Test
  void standardAridManagerShouldFilterOutMutationsByManagerDecision() {
    shouldFilterOutMutationsByManagerDecision(new StandardAridNodeMutationFilterFactory(() -> factory));
  }

  @Test
  void expertAridManagerShouldFilterOutMutationsByManagerDecision() {
    shouldFilterOutMutationsByManagerDecision(new ExpertAridNodeMutationFilterFactory((loader, mode) -> factory));
  }

  @Test
  void lineRangeAridManagerShouldFilterOutMutationsByManagerDecision() {
    shouldFilterOutMutationsByManagerDecision(new LineRangeAridNodeMutationFilterFactory((root, cp) -> factory));
  }

  private void shouldFilterOutMutationsByManagerDecision(MutationInterceptorFactory subject) {
    // Arrange
    int numberOfMutations = 10;
    val aridLines = Sets.newHashSet(1, 4, 5);
    val details = IntStream.range(1, numberOfMutations + 1)
        .mapToObj(line -> mutations(line, "UOI"))
        .flatMap(Function.identity())
        .collect(toList());
    val expected = details.stream()
        .filter(d -> !aridLines.contains(d.getLineNumber()))
        .toArray(MutationDetails[]::new);
    when(manager.decide(any())).then(invocation -> aridLines.contains(
        invocation.getArgument(0, MutationDetails.class).getLineNumber()));
    // Act
    val actual = subject
        .createInterceptor(params)
        .intercept(details, null);
    // Assert
    assertThat(actual, contains(expected));
  }
}
