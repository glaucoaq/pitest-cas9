package org.pitest.mutationtest.engine.cas9;

import static java.util.Collections.unmodifiableSet;
import static lombok.AccessLevel.PRIVATE;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.pitest.ast.ClassAstSource;
import org.pitest.ast.ClassAstSourceContext;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationEngine;
import org.pitest.mutationtest.engine.cas9.config.Cas9EngineConfiguration;
import org.pitest.mutationtest.engine.cas9.config.Cas9Mutater;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.util.PitError;

@RequiredArgsConstructor(access = PRIVATE)
public class Cas9MutationEngine implements MutationEngine {

  public static final String ENGINE_NAME = "cas9";

  @NonNull
  private final Set<MethodMutatorFactory> operators;

  @NonNull
  private final Predicate<MethodInfo> filter;

  @NonNull
  private final ClassAstSourceContext astContext;

  public static MutationEngine withConfig(Cas9EngineConfiguration config) {
    Set<MethodMutatorFactory> operators = unmodifiableSet(new HashSet<>(config.mutators()));
    return new Cas9MutationEngine(operators, config.methodFilter(), config.astContext());
  }

  @Override
  public Mutater createMutator(ClassByteArraySource source) {
    final ClassAstSource astSource = astContext.getAstSource()
        .orElseThrow(() -> new PitError("Cannot create CAS9 engine without AST source."));
    return new Cas9Mutater(filter, source, operators, astSource);
  }

  @Override
  public Collection<String> getMutatorNames() {
    return operators.stream()
        .map(MethodMutatorFactory::getName)
        .collect(Collectors.toSet());
  }

  @Override
  public String getName() {
    return ENGINE_NAME;
  }
}
