package org.pitest.mutationtest.arid.standard;

import static org.pitest.mutationtest.arid.NodeAridity.ABSTAIN;
import static org.pitest.mutationtest.arid.NodeAridity.ARID;
import static org.pitest.util.Log.getLogger;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithCondition;
import com.github.javaparser.ast.nodeTypes.NodeWithExpression;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import java.util.logging.Level;
import lombok.val;
import org.pitest.mutationtest.arid.AridityDetectionVoter;
import org.pitest.mutationtest.arid.NodeAridity;

class MethodCallAridityVoter implements AridityDetectionVoter {

  @Override
  public NodeAridity vote(Node node) {
    if (node instanceof NodeWithExpression<?>) {
      return vote(((NodeWithExpression<?>) node).getExpression());
    }
    if (node instanceof NodeWithCondition<?>) {
      return vote(((NodeWithCondition<?>) node).getCondition());
    }
    if (node instanceof AssignExpr) {
      return vote(((AssignExpr) node).getValue());
    }
    if (node instanceof VariableDeclarationExpr) {
      val hasArid = ((VariableDeclarationExpr) node).getVariables().stream()
          .map(declarator -> declarator.getInitializer().map(this::vote).orElse(ABSTAIN))
          .anyMatch(ARID::equals);
      return hasArid ? ARID : ABSTAIN;
    }
    if (node instanceof BinaryExpr) {
      val expr = ((BinaryExpr) node);
      return vote(expr.getLeft()) == ARID || vote(expr.getRight()) == ARID ? ARID : ABSTAIN;
    }
    return node instanceof MethodCallExpr
        ? arid((MethodCallExpr) node)
        : ABSTAIN;
  }

  private static NodeAridity arid(MethodCallExpr expr) {
    try {
      val packageName = expr.resolve().declaringType().getPackageName();
      return packageName.equals("java.lang") ? ARID : ABSTAIN;
    } catch (UnsolvedSymbolException e) {
      getLogger().log(Level.WARNING, "Could not resolve expression.", e);
      return ABSTAIN;
    }
  }
}
