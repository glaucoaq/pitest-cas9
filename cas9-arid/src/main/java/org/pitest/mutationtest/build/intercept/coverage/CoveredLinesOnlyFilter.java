package org.pitest.mutationtest.build.intercept.coverage;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import lombok.val;
import org.pitest.bytecode.analysis.ClassTree;
import org.pitest.coverage.CoverageDatabase;
import org.pitest.mutationtest.build.InterceptorType;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationDetails;

enum CoveredLinesOnlyFilter implements MutationInterceptor {

  FILTER;

  private CoverageDatabase coverage;

  synchronized void setCoverage(CoverageDatabase coverage) {
    this.coverage = coverage;
  }

  boolean hasCoverage(MutationDetails details) {
    val line = details.getClassLine();
    val tests = coverage.getTestsForClassLine(line);
    return tests != null && !tests.isEmpty();
  }

  @Override
  public InterceptorType type() {
    return InterceptorType.FILTER;
  }

  @Override
  public void begin(ClassTree clazz) { }

  @Override
  public Collection<MutationDetails> intercept(Collection<MutationDetails> mutations, Mutater mutater) {
    if (coverage == null) {
      return Collections.emptyList();
    }

    return mutations.stream()
        .filter(this::hasCoverage)
        .collect(Collectors.toList());
  }

  @Override
  public void end() { }
}
