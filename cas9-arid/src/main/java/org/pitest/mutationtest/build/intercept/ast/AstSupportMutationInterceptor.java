package org.pitest.mutationtest.build.intercept.ast;

import static java.util.stream.Collectors.toMap;

import com.github.javaparser.ast.Node;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import lombok.val;
import org.pitest.ast.ClassAstSourceContext;
import org.pitest.functional.Streams;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.util.ServiceLoader;

public interface AstSupportMutationInterceptor extends MutationInterceptor {

  default Collection<MutationDetails> intercept(Collection<MutationDetails> mutations, Mutater mutater) {
    val classAstSource = ServiceLoader.load(ClassAstSourceContext.class).stream()
        .map(ClassAstSourceContext::getAstSource)
        .flatMap(Streams::fromOptional)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Could not get the AST source from context."));

    Function<MutationIdentifier, MutationDetails> detailsById = mutations.stream()
        .collect(toMap(
            MutationDetails::getId,
            Function.identity()))
        ::get;

    return intercept(mutations, detailsById.andThen(classAstSource::getAstNode), mutater);
  }

  Collection<MutationDetails> intercept(Collection<MutationDetails> mutations,
      Function<MutationIdentifier, Optional<Node>> nodeById, Mutater mutater);
}
