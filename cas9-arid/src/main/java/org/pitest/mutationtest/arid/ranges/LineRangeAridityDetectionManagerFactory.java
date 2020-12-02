package org.pitest.mutationtest.arid.ranges;

import static lombok.AccessLevel.PRIVATE;

import com.github.javaparser.ast.Node;
import com.google.common.collect.Range;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.pitest.classinfo.ClassName;
import org.pitest.classpath.ClassPath;
import org.pitest.mutationtest.arid.AridityDetectionManager;
import org.pitest.mutationtest.arid.AridityDetectionManagerFactory;
import org.pitest.mutationtest.engine.MutationIdentifier;

@RequiredArgsConstructor(access = PRIVATE)
public class LineRangeAridityDetectionManagerFactory implements AridityDetectionManagerFactory {

  @NonNull
  private final LineRangeConfigReader configReader;

  private final ConcurrentMap<ClassName, Collection<Range<Integer>>> rangesByClassName = new ConcurrentHashMap<>();

  public static AridityDetectionManagerFactory of(Path rootPath, ClassPath classPath) {
    val configReader = new LineRangeConfigReader(rootPath, classPath);
    return new LineRangeAridityDetectionManagerFactory(configReader);
  }

  @Override
  public AridityDetectionManager createManager(Function<MutationIdentifier, Optional<Node>> mapper) {
    return details -> rangesByClassName
        .computeIfAbsent(details.getClassName(), configReader::getRanges)
        .stream()
        .anyMatch(range -> range.contains(details.getLineNumber()));
  }
}
