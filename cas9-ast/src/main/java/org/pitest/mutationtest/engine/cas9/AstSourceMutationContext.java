package org.pitest.mutationtest.engine.cas9;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Delegate;
import lombok.val;
import org.pitest.ast.ClassAstSource;
import org.pitest.mutationtest.engine.gregor.GregorClassContext;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.Type;

@Value(staticConstructor = "of")
class AstSourceMutationContext implements MutationContext, MethodAstInfoSource {

  private interface LineTrackingMethods {

    @SuppressWarnings("unused")
    void registerCurrentLine(int line);
  }

  @NonNull
  @Delegate(types = MutationContext.class, excludes = LineTrackingMethods.class)
  MutationContext mutationContext;

  @NonNull
  GregorClassContext classContext;

  @NonNull
  ClassAstSource source;

  AtomicInteger lastLineNumber = new AtomicInteger();

  @Override
  public void registerCurrentLine(int line) {
    lastLineNumber.set(line);
  }

  @Override
  public Optional<MethodAstInfo> getMethodAstInfo(MethodInfo methodInfo) {
    if (methodInfo.isGeneratedEnumMethod() || methodInfo.isStaticInitializer() || methodInfo.isSynthetic()) {
      return Optional.empty();
    }

    val classInfo = mutationContext.getClassInfo();
    val fileName = classContext.getFileName();

    if (classInfo == null || fileName == null) {
      return Optional.empty();
    }

    return source.getAst(classInfo.getName(), fileName)
        .flatMap(classAst -> findMemberByMethodInfo(classAst, methodInfo)
            .map(methodAst -> MethodAstInfo.of(classAst, methodAst)));
  }

  @Override
  public AstNodeTracker getAstNodeTracker(MethodInfo methodInfo) {
    BiFunction<Node, Integer, Boolean> containsLine = (node, line) ->
        node.getRange()
            .filter(range -> range.begin.line == line)
            .isPresent();

    return () ->
        lastLineNumber.get() == 0 ? Optional.empty() : getMethodAstInfo(methodInfo)
            .map(MethodAstInfo::getMethodAst)
            .flatMap(method -> method.findFirst(Node.class,
                node -> containsLine.apply(node, lastLineNumber.get())))
            .flatMap(node -> node instanceof Statement
                ? Optional.of((Statement) node)
                : node.findAncestor(Statement.class));
  }

  private static Optional<CallableDeclaration<?>> findMemberByMethodInfo(
      final TypeDeclaration<?> type, final MethodInfo methodInfo) {
    val paramTypes = Stream.of(Type.getArgumentTypes(methodInfo.getMethodDescriptor()))
        .map(Type::getClassName)
        .toArray(String[]::new);

    Predicate<BodyDeclaration<?>> isMethodDeclaration = BodyDeclaration::isMethodDeclaration;
    Predicate<BodyDeclaration<?>> isMethodWithName =
        ((Function<BodyDeclaration<?>, MethodDeclaration>) BodyDeclaration::asMethodDeclaration)
            .andThen(MethodDeclaration::getNameAsString)
            .andThen(methodInfo.getName()::equals)::apply;

    Predicate<BodyDeclaration<?>> matchesName =
        isMethodDeclaration.and(isMethodWithName).or(BodyDeclaration::isConstructorDeclaration);
    Predicate<BodyDeclaration<?>> matchesParameters =
        ((Function<BodyDeclaration<?>, CallableDeclaration<?>>) CallableDeclaration.class::cast)
            .andThen(member -> isCallableWithParameters(member, paramTypes))::apply;

    return type.getMembers().stream()
        .filter(matchesName.and(matchesParameters))
        .findFirst()
        .map(member -> (CallableDeclaration<?>) member);
  }

  private static boolean isCallableWithParameters(final CallableDeclaration<?> member, final String[] paramTypes) {
    if (member.getParameters().size() != paramTypes.length) {
      return false;
    }

    try {
      String[] resolvedTypes = member.getParameters().stream()
          .map(Parameter::resolve)
          .map(ResolvedParameterDeclaration::describeType)
          .toArray(String[]::new);
      return Arrays.equals(paramTypes, resolvedTypes);
    } catch (UnsolvedSymbolException e) {
      return false;
    }
  }
}
