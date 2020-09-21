package org.pitest.mutationtest.engine.cas9.mutators.sbr;

import static java.util.Arrays.asList;
import static org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator.VOID_METHOD_CALL_MUTATOR;

import java.util.Collection;
import org.pitest.mutationtest.engine.cas9.AstNodeTracker;
import org.pitest.mutationtest.engine.cas9.AstSupportMutatorFactory;
import org.pitest.mutationtest.engine.cas9.MethodAstInfo;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.MethodVisitor;

public enum SBRMutator implements AstSupportMutatorFactory {

  SBR_MUTATOR;

  public static Collection<MethodMutatorFactory> sbr() {
    return asList(VOID_METHOD_CALL_MUTATOR, SBR_MUTATOR);
  }

  @Override
  public MethodVisitor create(MutationContext context, AstNodeTracker astTracker, MethodAstInfo astInfo,
      MethodInfo methodInfo, MethodVisitor visitor) {
    return new SBRMethodVisitor(this, context, astTracker, visitor);
  }

  @Override
  public String getGloballyUniqueId() {
    return this.getClass().getName();
  }

  @Override
  public String getName() {
    return name();
  }
}
