package org.pitest.mutationtest.config;

import static java.util.Collections.emptyList;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import lombok.NonNull;
import lombok.val;
import org.pitest.ast.AbstractReusableClassAstSourceContext;
import org.pitest.util.EnvironmentUtils;

/**
 * Shares the input data used to initialize the AST in the main project
 * so it can be restored in minion process.
 * <p>
 *   This is required as the minion doesn't initiate the interceptors in the forked process.
 * </p>
 */
public class EnvironmentAstContext extends AbstractReusableClassAstSourceContext {

  private static final String SOURCE_DIRS_ENV = "org.pitest.ast.ClassAstSourceContext.EnvironmentAstContext#SRCDIRS";

  private static final String CLASSPATH_ELEMENTS_ENV = "org.pitest.ast.ClassAstSourceContext.EnvironmentAstContext#CLASSPATH";

  public static synchronized void saveSettings(@NonNull Collection<File> sourceDirs, @NonNull Collection<File> classPathElements) {
    EnvironmentUtils.setenv(SOURCE_DIRS_ENV, sourceDirs);
    EnvironmentUtils.setenv(CLASSPATH_ELEMENTS_ENV, classPathElements);
  }

  @Override
  protected synchronized Optional<ClassAstSourceSettings> getSettings() {
    val sourceDirs = EnvironmentUtils.getenv(SOURCE_DIRS_ENV, File::new, emptyList());
    val classPathElements = EnvironmentUtils.getenv(CLASSPATH_ELEMENTS_ENV, File::new, emptyList());
    return sourceDirs.isEmpty() || classPathElements.isEmpty()
        ? Optional.empty()
        : Optional.of(ClassAstSourceSettings.of(sourceDirs, classPathElements));
  }
}
