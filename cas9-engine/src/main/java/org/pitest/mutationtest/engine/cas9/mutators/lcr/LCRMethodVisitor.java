package org.pitest.mutationtest.engine.cas9.mutators.lcr;

import static com.github.javaparser.ast.expr.BinaryExpr.Operator.AND;
import static org.pitest.reloc.asm.Opcodes.IFEQ;

import com.github.javaparser.ast.expr.BinaryExpr;
import java.util.function.BiConsumer;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import org.pitest.bytecode.ASMVersion;
import org.pitest.mutationtest.engine.cas9.AstNodeTracker;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.Label;
import org.pitest.reloc.asm.MethodVisitor;

class LCRMethodVisitor extends MethodVisitor {

  private final MethodMutatorFactory factory;
  private final MutationContext context;
  private final AstNodeTracker tracker;
  private final Substitution substitution;

  @Builder(builderMethodName = "with")
  static class Substitution {

    @Getter
    @NonNull
    String description;

    @NonNull
    BiConsumer<MethodVisitor, Label> transformer;

    void transform(MethodVisitor visitor, Label label) {
      transformer.accept(visitor, label);
    }
  }

  public LCRMethodVisitor(@NonNull MethodMutatorFactory factory, @NonNull MutationContext context,
      @NonNull AstNodeTracker tracker, @NonNull Substitution substitution, @NonNull MethodVisitor visitor) {
    super(ASMVersion.ASM_VERSION, visitor);
    this.factory = factory;
    this.context = context;
    this.tracker = tracker;
    this.substitution = substitution;
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    if (canMutate(opcode)) {
      createMutationForLogicalConnector(opcode, label);
    } else {
      super.visitJumpInsn(opcode, label);
    }
  }

  private boolean canMutate(final int opcode) {
    return opcode == IFEQ && tracker
        .getCurrentStatement()
        .flatMap(stmt -> stmt.findFirst(BinaryExpr.class, expr -> expr.getOperator() == AND))
        .isPresent();
  }

  private void createMutationForLogicalConnector(final int opcode, final Label label) {
    val newId = context.registerMutation(factory, substitution.getDescription());
    if (this.context.shouldMutate(newId)) {
      substitution.transform(mv, label);
    } else {
      super.visitJumpInsn(opcode, label);
    }
  }
}
