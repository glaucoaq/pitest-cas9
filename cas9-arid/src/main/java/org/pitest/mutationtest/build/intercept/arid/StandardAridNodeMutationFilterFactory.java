package org.pitest.mutationtest.build.intercept.arid;

import static lombok.AccessLevel.PACKAGE;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.pitest.mutationtest.arid.AridityDetectionManagerFactory;
import org.pitest.mutationtest.arid.standard.StandardAridityDetectionManagerFactory;
import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.build.MutationInterceptorFactory;
import org.pitest.plugin.Feature;

@RequiredArgsConstructor(access = PACKAGE)
public class StandardAridNodeMutationFilterFactory implements MutationInterceptorFactory {

  static final String FEATURE_NAME = "FSARID";

  private final Supplier<AridityDetectionManagerFactory> createFactory;

  @SuppressWarnings("unused")
  public StandardAridNodeMutationFilterFactory() {
    this(StandardAridityDetectionManagerFactory::new);
  }

  @Override
  public MutationInterceptor createInterceptor(InterceptorParameters params) {
    return new AridNodeMutationFilter(createFactory.get());
  }

  @Override
  public Feature provides() {
    return Feature
        .named(FEATURE_NAME)
        .withOnByDefault(true)
        .withDescription("Filters out mutations based on predefined rules for arid nodes detection");
  }

  @Override
  public String description() {
    return "Arid nodes standard rules filter";
  }
}
