package org.pitest.mutationtest.build.intercept.limit;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Comparator;
import lombok.Value;
import org.pitest.bytecode.analysis.ClassTree;
import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.mutationtest.build.InterceptorType;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.build.MutationInterceptorFactory;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.plugin.Feature;
import org.pitest.plugin.FeatureParameter;

public class LimitNumberOfMutationsPerLineFilterFactory implements MutationInterceptorFactory {

  static final String FEATURE_NAME = "LINELIMIT";

  static final FeatureParameter LIMIT_PARAM = FeatureParameter
      .named("limit")
      .withDescription("Integer value for maximum mutations to create per line");

  static final FeatureParameter PRIORITY_PARAM = FeatureParameter
      .named("priority")
      .withDescription("Comma-separated list of operator names in prioritized order");

  static final Integer DEFAULT_LIMIT = 1;

  static final String DEFAULT_OPERATOR_PRIORITY = "ROR,LCR,SBR,AOR,UOI";

  @Override
  public MutationInterceptor createInterceptor(InterceptorParameters params) {
    int limit = params.getInteger(LIMIT_PARAM).orElse(DEFAULT_LIMIT);
    String priority = params.getString(PRIORITY_PARAM).orElse(DEFAULT_OPERATOR_PRIORITY);
    Comparator<? super MutationDetails> prioritiser = MutationPrioritiser
        .of(priority)
        .makeMutatorPrioritiser();
    return LimitNumberOfMutationsPerLineFilter.of(limit, prioritiser);
  }

  @Override
  public Feature provides() {
    return Feature.named(FEATURE_NAME)
        .withOnByDefault(true)
        .withDescription("Limits the maximum number of mutations per line")
        .withParameter(LIMIT_PARAM);
  }

  @Override
  public String description() {
    return "Max mutations per line limit";
  }

 @Value(staticConstructor = "of")
  static class LimitNumberOfMutationsPerLineFilter implements MutationInterceptor {

    int maxMutationsPerLine;

    Comparator<? super MutationDetails> byMutatorPriority;

    @Override
    public InterceptorType type() {
      return InterceptorType.FILTER;
    }

    @Override
    public void begin(ClassTree clazz) { }

    @Override
    public Collection<MutationDetails> intercept(Collection<MutationDetails> mutations, Mutater mutater) {
      return mutations.stream()
          .collect(groupingBy(MutationDetails::getLineNumber))
          .values().stream()
          .peek(details -> details.sort(byMutatorPriority))
          .flatMap(details -> details.stream().limit(maxMutationsPerLine))
          .collect(toList());
    }

    @Override
    public void end() { }
  }
}
