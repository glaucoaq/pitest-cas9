package org.pitest.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.NonNull;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.engine.MutationDetails;

public interface ClassAstSource {

  Optional<TypeDeclaration<?>> getAst(ClassName className, String fileName);

  default Optional<TypeDeclaration<?>> getAst(String clazz, String fileName) {
    return getAst(ClassName.fromString(clazz), fileName);
  }

  default Optional<TypeDeclaration<?>> getAst(MutationDetails details) {
    return getAst(details.getClassName(), details.getFilename());
  }

  default Optional<Node> getAstNode(@NonNull MutationDetails details) {
    final int lineNumber = details.getLineNumber();
    final Predicate<Statement> containsLine = node ->
        node.getRange()
            .filter(range -> range.begin.line == lineNumber)
            .isPresent();
    return getAst(details)
        .flatMap(type -> type.findFirst(Statement.class, containsLine))
        .map(Node.class::cast);
  }
}
