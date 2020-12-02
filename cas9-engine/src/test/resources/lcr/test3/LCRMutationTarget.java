class LCRMutationTarget {

  void doIt(boolean a, boolean b) {
    double x = Math.random();
    double y = Math.random();
    boolean t = (a && b);
    boolean u = (x > y && b);
    boolean v = (a && y == x);
    boolean r = (t && u && v);
  }

  void testIt(boolean p, boolean q, Object o) {
    Object b = new Object();
    boolean t = false;
    if (p || q)
      o.equals(b);
  }
}
