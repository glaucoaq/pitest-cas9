package org.pitest.mutationtest.arid.standard;

@SuppressWarnings("unused")
final class StandardAridMutationTargets {

  private StandardAridMutationTargets() {
    throw new UnsupportedOperationException();
  }

  // lines: 18, 19, 23, 27, 28, 29, 34, 35, 36
  static final class HasAridAndRelevantNodes {

    private int index;

    private int previous;

    HasAridAndRelevantNodes() {
      index = 1;
      previous = 1;
    }

    public int getPrevious() {
      return previous;
    }

    int next() {
      int next = previous * ++index;
      previous = next;
      return next;
    }

    @Override
    public String toString() {
      String res = previous + " (" + index + ")";
      boolean begin = "1 (1)".equals(res);
      return begin ? "-" : res;
    }
  }

  // lines: 50, 54, 58, 62, 66, 67, 68
  static final class HasOnlyAridNodes {

    enum Messages { EMPTY }

    private int code;

    private Object message;

    public int getCode() {
      return code;
    }

    public void setCode(int code) {
      this.code = code;
    }

    public Object getMessage() {
      return message;
    }

    public void setMessage(Object message) {
      this.message = message;
    }

    public void print() {
      String empty = Messages.EMPTY.name();
      String msg = message.toString();
      throw new IllegalStateException(empty + msg);
    }
  }

  // lines: 80, 81, 85, 86, 87
  static final class HasOnlyRelevantNodes {

    private final int seed;

    private int previous;

    HasOnlyRelevantNodes(int seed) {
      this.seed = seed;
      previous = seed;
    }

    int getNext() {
      int next = seed + previous;
      previous = next;
      return next;
    }
  }

  // lines: 96, 97, 99, 100, 101
  static final class HasMixedCompoundNodes {

    void doIt(boolean p, String s) {
      boolean q = false;
      if (p) {
        q = !"EMPTY".equals(s);
      }
      if (p && q) {
        String r = String.valueOf(q);
        System.out.print(s);
      }
    }
  }
}
