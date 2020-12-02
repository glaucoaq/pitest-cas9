package org.pitest.mutationtest.build.intercept.coverage;

import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.build.MutationInterceptorFactory;
import org.pitest.plugin.Feature;

public class CoveredLinesOnlyFilterFactory implements MutationInterceptorFactory {

  @Override
  public MutationInterceptor createInterceptor(InterceptorParameters params) {
    return CoveredLinesOnlyFilter.FILTER;
  }

  @Override
  public Feature provides() {
    return Feature.named("FCCOVL")
        .withOnByDefault(true)
        .withDescription("Filters out mutations in lines not covered by any unit test");
  }

  @Override
  public String description() {
    return "Uncovered lines filter";
  }
}
