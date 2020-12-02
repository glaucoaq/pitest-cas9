package org.pitest.ast;

import java.util.Optional;

public interface ClassAstSourceContext {

  Optional<ClassAstSource> getAstSource();
}
