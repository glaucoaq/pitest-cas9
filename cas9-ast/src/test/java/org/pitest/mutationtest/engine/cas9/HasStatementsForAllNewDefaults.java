package org.pitest.mutationtest.engine.cas9;

@SuppressWarnings("unused")
class HasStatementsForAllNewDefaults {

  int doIt(int j) {
    for (int i = 0; i < 10; i++) // CONDITIONALS_BOUNDARY, INCREMENTS
      j = i ^ j; // MATH

    doNothing(); // VOID_METHOD_CALL

    int r = -j; // INVERT_NEGS
    return r; // PRIMITIVE_RETURN_VALS
  }

  boolean testIt(boolean b) {
    return b; // BOOLEAN_TRUE_RETURN, BOOLEAN_FALSE_RETURN
  }

  String echoIt(String a) {
    return a; // EMPTY_RETURN_VALUES
  }

  Object echoIt(Object o) {
    return o; // NULL_RETURN_VALUES
  }

  void doNothing() {}
}
