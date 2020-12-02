package org.pitest.mutationtest.arid.expert;

import com.github.javaparser.ast.Node;
import java.util.Optional;
import java.util.function.Function;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.pitest.mutationtest.arid.AridityDetectionManager;
import org.pitest.mutationtest.arid.AridityDetectionManagerFactory;
import org.pitest.mutationtest.arid.AridityDetectionVoter;
import org.pitest.mutationtest.arid.managers.AffirmativeManager;
import org.pitest.mutationtest.arid.managers.ConsensusManager;
import org.pitest.mutationtest.arid.managers.UnanimousManager;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.util.ServiceLoader;

@RequiredArgsConstructor
public class ExpertAridityDetectionManagerFactory implements AridityDetectionManagerFactory {

  @NonNull
  private final ClassLoader loader;

  @NonNull
  private final AridityDetectionMode detectionMode;

  @Override
  public AridityDetectionManager createManager(Function<MutationIdentifier, Optional<Node>> mapper) {
    val voters = ServiceLoader.load(AridityDetectionVoter.class, loader);
    switch (detectionMode) {
      case AFFIRMATIVE:
        return new AffirmativeManager(mapper, voters);
      case UNANIMOUS:
        return new UnanimousManager(mapper, voters);
      case CONSENSUS:
        return new ConsensusManager(mapper, voters);
      default:
        throw new IllegalStateException();
    }
  }
}
