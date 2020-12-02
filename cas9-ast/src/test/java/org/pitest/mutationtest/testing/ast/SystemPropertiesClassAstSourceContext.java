package org.pitest.mutationtest.testing.ast;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.val;
import org.pitest.ast.AbstractReusableClassAstSourceContext;

public class SystemPropertiesClassAstSourceContext extends AbstractReusableClassAstSourceContext {

  private static final String SOURCE_DIRS_PROPERTY = "cas9-test.sources";

  private static final String CLASSES_DIRS_PROPERTY = "cas9-test.classes";

  private static final String CLASSPATH_PROPERTY = "cas9-test.classpath";

  @Override
  protected Optional<ClassAstSourceSettings> getSettings() {
    val sourceDir = System.getProperty(SOURCE_DIRS_PROPERTY, "src/test/java");
    val classesDir = System.getProperty(CLASSES_DIRS_PROPERTY, "target/classes");
    val classpath = new HashSet<>(asList(System.getProperty(CLASSPATH_PROPERTY, "").split(":")));

    classpath.add(classesDir);

    val sourceDirs = Stream.of(sourceDir).map(File::new).collect(toList());
    val classpathElements = classpath.stream().map(File::new).collect(toList());

    ClassAstSourceSettings settings = ClassAstSourceSettings.of(sourceDirs, classpathElements);

    return Optional.of(settings);
  }

  @Override
  protected boolean isReusable() {
    return false;
  }
}
