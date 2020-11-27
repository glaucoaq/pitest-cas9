package org.pitest.mutationtest.arid.managers;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.partitioningBy;

import com.github.javaparser.ast.Node;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.val;
import org.pitest.mutationtest.arid.AridityDetectionVoter;
import org.pitest.mutationtest.arid.NodeAridity;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;

public class ConsensusManager extends AbstractAridityDetectionManager {

  public ConsensusManager(Function<MutationIdentifier, Optional<Node>> mapper,
      Collection<AridityDetectionVoter> voters) {
    super(mapper, voters);
  }

  @Override
  protected boolean decide(Stream<NodeAridity> votes) {
    val poll = votes.collect(
        partitioningBy(
            NodeAridity.ARID::equals,
            counting()));
    val aridVotes = poll.getOrDefault(true, 0L);
    val relevantVotes = poll.getOrDefault(false, 0L);
    return aridVotes > relevantVotes;
  }

  @Override
  protected NodeAridity defaultAridity(MutationDetails details) {
    return NodeAridity.ARID;
  }
}
