package org.pitest.mutationtest.engine.cas9;

import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PROTECTED;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
import org.pitest.ast.ClassAstSource;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassWriterFactory;
import org.pitest.mutationtest.engine.gregor.AbstractGregorMutater;
import org.pitest.mutationtest.engine.gregor.GregorClassContext;
import org.pitest.mutationtest.engine.gregor.GregorClassWriterFactory;
import org.pitest.mutationtest.engine.gregor.GregorMutatingClassVisitor;
import org.pitest.mutationtest.engine.gregor.LineTrackingMethodVisitor;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.ClassVisitor;
import org.pitest.reloc.asm.ClassWriter;
import org.pitest.reloc.asm.MethodVisitor;

public class AstGregorMutater extends AbstractGregorMutater {

  private final ClassWriterFactory writerFactory;

  @Getter(PROTECTED)
  private final ClassAstSource classAstSource;

  public AstGregorMutater(Predicate<MethodInfo> filter, ClassByteArraySource byteSource,
      Collection<MethodMutatorFactory> mutators, ClassAstSource classAstSource) {
    super(filter, byteSource, mutators);
    writerFactory = new GregorClassWriterFactory(byteSource);
    this.classAstSource = classAstSource;
  }

  @Override
  protected GregorClassContext createContext() {
    return new GregorClassContext();
  }

  @Override
  protected ClassWriter createWriter(String className) {
    return writerFactory.createWriter(className);
  }

  @Override
  protected GregorMutatingClassVisitor createMutatingVisitor(ClassVisitor visitor, GregorClassContext context,
      Predicate<MethodInfo> filter, Collection<MethodMutatorFactory> mutators) {
    UnaryOperator<MutationContext> toAstSource = ctx -> AstSourceMutationContext.of(ctx, context, classAstSource);
    val decoratedMutators = mutators.stream()
        .map(mutator -> (MethodMutatorFactory) MutationContextDecoratorFactory.of(mutator, toAstSource))
        .collect(toList());
    return new GregorMutatingClassVisitor(visitor, context, filter, decoratedMutators);
  }

  @Value(staticConstructor = "of")
  private static class MutationContextDecoratorFactory implements MethodMutatorFactory {

    @NonNull
    MethodMutatorFactory factory;

    @NonNull
    UnaryOperator<MutationContext> decorate;

    @Override
    public MethodVisitor create(MutationContext context, MethodInfo methodInfo, MethodVisitor visitor) {
      val decoratedContext = decorate.apply(context);
      val mutatorVisitor = factory.create(decoratedContext, methodInfo, visitor);
      return new LineTrackingMethodVisitor(decoratedContext, mutatorVisitor);
    }

    @Override
    public String getGloballyUniqueId() {
      return factory.getGloballyUniqueId();
    }

    @Override
    public String getName() {
      return factory.getName();
    }
  }
}
