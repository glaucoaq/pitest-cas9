package org.pitest.mutationtest.arid.ranges;

import static java.lang.Integer.parseInt;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.Range;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.pitest.classinfo.ClassName;
import org.pitest.classpath.ClassPath;

@RequiredArgsConstructor
class LineRangeConfigReader {

  static final String ARID_RANGE_PROPERTY = "arid-lines";

  static final String ARID_RANGE_SEPARATOR = ",";

  static final String ARID_RANGE_VALUES_SEPARATOR = "-";

  @NonNull
  private final Path rootPath;

  @NonNull
  private final ClassPath classPath;

  Collection<Range<Integer>> getRanges(@NonNull final ClassName name) {
    return findConfigFile(name)
        .map(this::readConfigFile)
        .orElse(emptyList());
  }

  @SneakyThrows
  private Optional<Path> findConfigFile(ClassName name) {
    val resourceName = rootPath.resolve(name.asJavaName() + ".properties");
    val resourceUrl = classPath.findResource(resourceName.toString());
    return resourceUrl == null
        ? Optional.empty()
        : Optional.of(Paths.get(resourceUrl.toURI()));
  }

  private Collection<Range<Integer>> readConfigFile(@NonNull final Path resourcePath) {
    val properties = new Properties();
    try (Reader reader = Files.newBufferedReader(resourcePath)) {
      properties.load(reader);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    val rangesValues = properties.getProperty(ARID_RANGE_PROPERTY);
    return rangesValues == null ? emptyList() : Stream.of(rangesValues.split(ARID_RANGE_SEPARATOR))
        .map(values -> values.split(ARID_RANGE_VALUES_SEPARATOR))
        .map(values -> Range.closed(parseInt(values[0]), parseInt(values[1])))
        .collect(toList());
  }
}
