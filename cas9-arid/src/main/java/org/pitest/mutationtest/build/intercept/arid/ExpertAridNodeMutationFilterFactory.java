package org.pitest.mutationtest.build.intercept.arid;

import static lombok.AccessLevel.PACKAGE;

import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.pitest.mutationtest.arid.AridityDetectionManagerFactory;
import org.pitest.mutationtest.arid.expert.AridityDetectionMode;
import org.pitest.mutationtest.arid.expert.ExpertAridityDetectionManagerFactory;
import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.build.MutationInterceptorFactory;
import org.pitest.plugin.Feature;
import org.pitest.plugin.FeatureParameter;
import org.pitest.util.IsolationUtils;

@RequiredArgsConstructor(access = PACKAGE)
public class ExpertAridNodeMutationFilterFactory implements MutationInterceptorFactory {

  static final String FEATURE_NAME = "FEARID";

  static final FeatureParameter MODE_PARAM = FeatureParameter
      .named("mode")
      .withDescription("Arid node detection mode");

  private final BiFunction<ClassLoader, AridityDetectionMode, AridityDetectionManagerFactory> createFactory;

  @SuppressWarnings("unused")
  public ExpertAridNodeMutationFilterFactory() {
    this(ExpertAridityDetectionManagerFactory::new);
  }

  @Override
  public MutationInterceptor createInterceptor(InterceptorParameters params) {
    val loader = IsolationUtils.getContextClassLoader();
    val mode = params.getString(MODE_PARAM)
        .map(AridityDetectionMode::valueOf)
        .orElse(AridityDetectionMode.AFFIRMATIVE);
    val factory = createFactory.apply(loader, mode);
    return new AridNodeMutationFilter(factory);
  }

  @Override
  public Feature provides() {
    return Feature.named(FEATURE_NAME)
        .withParameter(MODE_PARAM)
        .withDescription("Filters out mutations based on custom rules for arid nodes detection");
  }

  @Override
  public String description() {
    return "Arid nodes standard rules filter";
  }
}
