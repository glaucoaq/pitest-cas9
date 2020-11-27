class SBRMutationTarget {

  Random random = new Random();

  void doIt(int a, int b) {
    System.out.println("begin");
    int c = this.random.nextInt();
    if (a > 0) {
      System.out.println("a = " + a);
      while (a < b) {
        if (a == c) {
          System.out.println("break!");
          break;
        }
        a++;
      }
    }
    System.out.println("done");
  }
}
