package org.pitest.mutationtest.engine.cas9.config;

import java.util.Collection;
import java.util.function.Predicate;
import org.pitest.ast.ClassAstSource;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.mutationtest.engine.cas9.AstGregorMutater;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;

public class Cas9Mutater extends AstGregorMutater {

  public Cas9Mutater(Predicate<MethodInfo> filter, ClassByteArraySource byteSource,
      Collection<MethodMutatorFactory> mutators, ClassAstSource classAstSource) {
    super(filter, byteSource, mutators, classAstSource);
  }

  @Override
  public ClassAstSource getClassAstSource() {
    return super.getClassAstSource();
  }
}
