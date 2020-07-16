package org.pitest.mutationtest.arid;

import com.github.javaparser.ast.Node;
import java.util.function.Function;
import org.pitest.mutationtest.engine.MutationIdentifier;

public interface AridityDetectionManagerFactory {

  AridityDetectionManager createManager(Function<MutationIdentifier, Node> mapper);
}
