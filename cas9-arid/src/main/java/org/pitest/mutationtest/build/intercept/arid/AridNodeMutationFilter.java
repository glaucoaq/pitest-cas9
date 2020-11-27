package org.pitest.mutationtest.build.intercept.arid;

import static org.pitest.functional.prelude.Prelude.not;

import com.github.javaparser.ast.Node;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.pitest.bytecode.analysis.ClassTree;
import org.pitest.mutationtest.arid.AridityDetectionManagerFactory;
import org.pitest.mutationtest.build.InterceptorType;
import org.pitest.mutationtest.build.intercept.ast.AstSupportMutationInterceptor;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;

@RequiredArgsConstructor
class AridNodeMutationFilter implements AstSupportMutationInterceptor {

  @NonNull
  private final AridityDetectionManagerFactory factory;

  @Override
  public InterceptorType type() {
    return InterceptorType.FILTER;
  }

  @Override
  public Collection<MutationDetails> intercept(@NonNull Collection<MutationDetails> mutations,
      @NonNull Function<MutationIdentifier, Optional<Node>> nodeById, Mutater mutater) {
    Predicate<MutationDetails> arid = factory.createManager(nodeById)::decide;
    return mutations.stream()
        .filter(not(arid))
        .collect(Collectors.toList());
  }

  @Override
  public void begin(ClassTree clazz) { }

  @Override
  public void end() { }
}
