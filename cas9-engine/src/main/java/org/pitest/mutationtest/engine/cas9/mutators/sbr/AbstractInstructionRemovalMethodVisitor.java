package org.pitest.mutationtest.engine.cas9.mutators.sbr;

import java.util.concurrent.atomic.LongAdder;
import org.pitest.bytecode.ASMVersion;
import org.pitest.reloc.asm.Handle;
import org.pitest.reloc.asm.Label;
import org.pitest.reloc.asm.MethodVisitor;

abstract class AbstractInstructionRemovalMethodVisitor extends MethodVisitor {

  private final LongAdder counter = new LongAdder();

  public AbstractInstructionRemovalMethodVisitor(MethodVisitor visitor) {
    super(ASMVersion.ASM_VERSION, visitor);
  }

  @Override
  public void visitFrame(final int type, final int nLocal,
      final Object[] local, final int nStack, final Object[] stack) {
    counter.increment();
    super.visitFrame(type, nLocal, local, nStack, stack);
  }

  @Override
  public void visitInsn(final int opcode) {
    counter.increment();
    if (shouldPreserveInsn(counter.intValue())) {
      super.visitInsn(opcode);
    }
  }

  @Override
  public void visitIntInsn(final int opcode, final int operand) {
    counter.increment();
    if (shouldPreserveInsn(counter.intValue())) {
      super.visitIntInsn(opcode, operand);
    }
  }

  @Override
  public void visitVarInsn(final int opcode, final int var) {
    counter.increment();
    if (shouldPreserveInsn(counter.intValue())) {
      super.visitVarInsn(opcode, var);
    }
  }

  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    counter.increment();
    if (shouldPreserveInsn(counter.intValue())) {
      super.visitTypeInsn(opcode, type);
    }
  }

  @Override
  public void visitFieldInsn(final int opcode, final String owner,
      final String name, final String desc) {
    counter.increment();
    if (shouldPreserveInsn(counter.intValue())) {
      super.visitFieldInsn(opcode, owner, name, desc);
    }
  }

  @Override
  public void visitMethodInsn(final int opcode, final String owner,
      final String name, final String desc, boolean itf) {
    counter.increment();
    if (shouldPreserveInsn(counter.intValue())) {
      super.visitMethodInsn(opcode, owner, name, desc, itf);
    }
  }

  @Override
  public void visitInvokeDynamicInsn(final String name, final String desc,
      final Handle bsm, final Object... bsmArgs) {
    counter.increment();
    if (shouldPreserveInsn(counter.intValue())) {
      super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }
  }

  @Override
  public void visitJumpInsn(final int opcode, final Label label) {
    counter.increment();
    if (shouldPreserveInsn(counter.intValue())) {
      super.visitJumpInsn(opcode, label);
    }
  }

  @Override
  public void visitLabel(final Label label) {
    counter.increment();
    if (shouldPreserveInsn(counter.intValue())) {
      super.visitLabel(label);
    }
  }

  @Override
  public void visitLdcInsn(final Object cst) {
    counter.increment();
    if (shouldPreserveInsn(counter.intValue())) {
      super.visitLdcInsn(cst);
    }
  }

  @Override
  public void visitIincInsn(final int var, final int increment) {
    counter.increment();
    if (shouldPreserveInsn(counter.intValue())) {
      super.visitIincInsn(var, increment);
    }
  }

  @Override
  public void visitTableSwitchInsn(final int min, final int max,
      final Label dflt, final Label... labels) {
    counter.increment();
    if (shouldPreserveInsn(counter.intValue())) {
      super.visitTableSwitchInsn(min, max, dflt, labels);
    }
  }

  @Override
  public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
      final Label[] labels) {
    counter.increment();
    if (shouldPreserveInsn(counter.intValue())) {
      super.visitLookupSwitchInsn(dflt, keys, labels);
    }
  }

  @Override
  public void visitMultiANewArrayInsn(final String desc, final int dims) {
    counter.increment();
    if (shouldPreserveInsn(counter.intValue())) {
      super.visitMultiANewArrayInsn(desc, dims);
    }
  }

  @Override
  public void visitLineNumber(final int line, final Label start) {
    counter.increment();
    super.visitLineNumber(line, start);
  }

  protected abstract boolean shouldPreserveInsn(int index);
}
