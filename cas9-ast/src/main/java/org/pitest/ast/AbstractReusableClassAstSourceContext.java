package org.pitest.ast;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import lombok.Value;

public abstract class AbstractReusableClassAstSourceContext implements ClassAstSourceContext {

  private static ClassAstSource sharedAstSource;

  @Value(staticConstructor = "of")
  protected static class ClassAstSourceSettings {

    Collection<File> sourceDirs;

    Collection<File> classPathElements;
  }

  @Override
  public synchronized final Optional<ClassAstSource> getAstSource() {
    if (sharedAstSource == null) {
      return getSettings()
          .map(settings -> new ProjectBuildConfigAstSource(settings.getSourceDirs(), settings.getClassPathElements()))
          .map(CachingClassAstSource::new)
          .map(this::getSharedIfReusable);
    }
    return Optional.of(sharedAstSource);
  }

  private ClassAstSource getSharedIfReusable(ClassAstSource astSource) {
    if (isReusable()) {
      AbstractReusableClassAstSourceContext.sharedAstSource = astSource;
    }
    return astSource;
  }

  protected abstract Optional<ClassAstSourceSettings> getSettings();

  protected boolean isReusable() {
    return true;
  }
}
