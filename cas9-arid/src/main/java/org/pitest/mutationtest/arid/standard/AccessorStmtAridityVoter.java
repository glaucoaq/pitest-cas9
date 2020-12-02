package org.pitest.mutationtest.arid.standard;

import static com.google.common.base.Predicates.compose;
import static java.util.regex.Pattern.compile;
import static org.pitest.functional.prelude.Prelude.not;
import static org.pitest.functional.prelude.Prelude.or;
import static org.pitest.util.Log.getLogger;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Stream;
import lombok.val;
import org.pitest.mutationtest.arid.AridityDetectionVoter;
import org.pitest.mutationtest.arid.NodeAridity;

class AccessorStmtAridityVoter implements AridityDetectionVoter {

  private static final Predicate<NodeList<?>> HAS_ZERO_OR_ONE_PARAMS = or(Collection::isEmpty, col -> col.size() == 1);

  private static final Predicate<String> HAS_ACCESSOR_NAME = compile("^(get|is|set)([A-Z]|_[A-Za-z])").asPredicate();

  @Override
  public NodeAridity vote(Node node) {
    return node.findAncestor(MethodDeclaration.class,
        not(MethodDeclaration::isConstructorDeclaration)
            .and(not(MethodDeclaration::isStatic))
            .and(compose(HAS_ZERO_OR_ONE_PARAMS::test, MethodDeclaration::getParameters))
            .and(compose(HAS_ACCESSOR_NAME::test, MethodDeclaration::getNameAsString)))
        .map(AccessorStmtAridityVoter::arid)
        .orElse(NodeAridity.ABSTAIN);
  }

  private static NodeAridity arid(MethodDeclaration method) {
    Predicate<Expression> isFieldAccess = or(
        expr -> expr.toFieldAccessExpr()
            .map(FieldAccessExpr::getScope)
            .filter(Expression::isThisExpr)
            .isPresent(),
        expr -> expr.toNameExpr()
            .map(AccessorStmtAridityVoter::safeResolve)
            .filter(ResolvedDeclaration::isField)
            .isPresent());

    Predicate<Statement> isFieldAssign = stmt -> stmt.toExpressionStmt()
        .map(ExpressionStmt::getExpression)
        .flatMap(Expression::toAssignExpr)
        .map(AssignExpr::getTarget)
        .filter(isFieldAccess)
        .isPresent();

    Predicate<Statement> isAccessorReturn = stmt -> stmt.toReturnStmt()
        .flatMap(ReturnStmt::getExpression)
        .filter(or(Expression::isThisExpr, isFieldAccess))
        .isPresent();

    val hasOnlyAccessorStatements = method.getBody()
        .map(BlockStmt::getStatements)
        .map(Collection::stream)
        .orElse(Stream.empty())
        .allMatch(isFieldAssign.or(isAccessorReturn));

    return hasOnlyAccessorStatements ? NodeAridity.ARID : NodeAridity.ABSTAIN;
  }

  private static ResolvedDeclaration safeResolve(NameExpr expr) {
    try {
      return expr.resolve();
    } catch (UnsolvedSymbolException e) {
      getLogger().log(Level.WARNING, "Could not resolve expression.", e);
      return expr::getNameAsString;
    }
  }
}
