package org.pitest.mutationtest.build.intercept.ast;

import static org.pitest.mutationtest.build.intercept.ast.ClassAstSettingsInterceptor.INTERCEPTOR;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import java.util.Optional;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.engine.MutationDetails;

public interface ClassAstSource {

  Optional<ClassOrInterfaceDeclaration> getAst(ClassName className, String fileName);

  default Optional<ClassOrInterfaceDeclaration> getAst(String clazz, String fileName) {
    return getAst(ClassName.fromString(clazz), fileName);
  }

  default Optional<ClassOrInterfaceDeclaration> getAst(MutationDetails details) {
    return getAst(details.getClassName(), details.getFilename());
  }

  static ClassAstSource getDefault() {
    return INTERCEPTOR.getAstSource()
        .orElse((name, file) -> Optional.empty());
  }
}
