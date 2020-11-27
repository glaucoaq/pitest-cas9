package org.pitest.mutationtest.engine.cas9.mutators.lcr;

import static java.util.Arrays.asList;
import static org.pitest.reloc.asm.Opcodes.GOTO;
import static org.pitest.reloc.asm.Opcodes.POP;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.pitest.mutationtest.engine.cas9.AstNodeTracker;
import org.pitest.mutationtest.engine.cas9.AstSupportMutatorFactory;
import org.pitest.mutationtest.engine.cas9.MethodAstInfo;
import org.pitest.mutationtest.engine.cas9.mutators.lcr.LCRMethodVisitor.Substitution;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.MethodVisitor;

@RequiredArgsConstructor
public enum LCRMutator implements AstSupportMutatorFactory {

  LCR1_MUTATOR(Substitution.with()
      .description("replace logical connector with single operand")
      .transformer((visitor, label) -> visitor.visitInsn(POP))
      .build()),

  LCR2_MUTATOR(Substitution.with()
      .description("replace logical connector with false")
      .transformer((visitor, label) -> {
        visitor.visitInsn(POP);
        visitor.visitJumpInsn(GOTO, label);
      })
      .build());

  private final Substitution substitution;

  public static Collection<MethodMutatorFactory> lcr() {
    return asList(LCR1_MUTATOR, LCR2_MUTATOR);
  }

  @Override
  public MethodVisitor create(MutationContext context, AstNodeTracker astTracker, MethodAstInfo astInfo,
      MethodInfo methodInfo, MethodVisitor visitor) {
    return new LCRMethodVisitor(this, context, astTracker, substitution, visitor);
  }

  @Override
  public String getGloballyUniqueId() {
    return getClass().getName() + "." + name();
  }

  @Override
  public String getName() {
    return name();
  }
}
