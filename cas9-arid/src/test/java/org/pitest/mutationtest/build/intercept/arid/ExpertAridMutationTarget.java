package org.pitest.mutationtest.build.intercept.arid;

class ExpertAridMutationTarget {

  static Integer MUTATED_LINE = 8;

  String getIt() {
    return "This is the line " + MUTATED_LINE;
  }
}
