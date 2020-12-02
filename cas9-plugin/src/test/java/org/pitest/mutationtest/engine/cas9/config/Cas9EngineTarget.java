package org.pitest.mutationtest.engine.cas9.config;

class Cas9EngineTarget {

  static final int AOR_LINE = 12;

  static final int ROR_LINE = AOR_LINE + 5;

  @SuppressWarnings("unused")
  void doIt(final int a) {
    double x = Math.random();
    double y = a * x; // AOR
    skipIt(x, y);
  }

  void skipIt(final double a, final double b) {
    if (a < b) { // ROR, skipped by method exclusion
      System.out.println("a: " + a);
    }
  }
}
