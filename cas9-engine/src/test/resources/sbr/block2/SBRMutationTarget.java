class SBRMutationTarget {

  Random random = new Random();

  void doIt(int a, int b) {
    System.out.println("begin");
    if (a == 0)
      a = this.random.nextInt();
    int c = this.random.nextInt();
    System.out.println("done");
  }
}
