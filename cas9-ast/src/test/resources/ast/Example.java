package org.pitest.ast.test;

import java.util.Random;

public class Example {

  private String message;

  void doIt() {
    int r = new Random().nextInt();
    // ClassAstSourceTest.TARGET_LINE_NUMBER
    message = "This is a random number: " + r;
    System.out.println(message);
  }
}
