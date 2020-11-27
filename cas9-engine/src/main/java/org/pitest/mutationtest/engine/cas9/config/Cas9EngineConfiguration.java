package org.pitest.mutationtest.engine.cas9.config;

import org.pitest.ast.ClassAstSourceContext;
import org.pitest.mutationtest.engine.gregor.MutationEngineConfiguration;

public interface Cas9EngineConfiguration extends MutationEngineConfiguration {

  ClassAstSourceContext astContext();
}
