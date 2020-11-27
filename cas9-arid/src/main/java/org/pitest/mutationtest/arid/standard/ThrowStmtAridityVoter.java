package org.pitest.mutationtest.arid.standard;

import static org.pitest.mutationtest.arid.NodeAridity.ABSTAIN;
import static org.pitest.mutationtest.arid.NodeAridity.ARID;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.ThrowStmt;
import org.pitest.mutationtest.arid.AridityDetectionVoter;
import org.pitest.mutationtest.arid.NodeAridity;

class ThrowStmtAridityVoter implements AridityDetectionVoter {

  @Override
  public NodeAridity vote(Node node) {
    return node instanceof ThrowStmt || node.findAncestor(ThrowStmt.class).isPresent() ? ARID : ABSTAIN;
  }
}
