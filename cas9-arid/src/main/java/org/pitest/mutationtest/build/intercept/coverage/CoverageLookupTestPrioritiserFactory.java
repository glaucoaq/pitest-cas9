package org.pitest.mutationtest.build.intercept.coverage;

import static org.pitest.mutationtest.build.intercept.coverage.CoveredLinesOnlyFilter.FILTER;

import java.util.Properties;
import org.pitest.classpath.CodeSource;
import org.pitest.coverage.CoverageDatabase;
import org.pitest.mutationtest.build.DefaultTestPrioritiser;
import org.pitest.mutationtest.build.TestPrioritiser;
import org.pitest.mutationtest.build.TestPrioritiserFactory;

public class CoverageLookupTestPrioritiserFactory implements TestPrioritiserFactory {

  @Override
  public TestPrioritiser makeTestPrioritiser(Properties props, CodeSource code, CoverageDatabase coverage) {
    FILTER.setCoverage(coverage);
    return new DefaultTestPrioritiser(coverage);
  }

  @Override
  public String description() {
    return "Coverage capturing prioritiser";
  }
}
