package org.pitest.ast;

import java.io.File;
import java.util.Collection;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ClassAstSourceFactory {

  public ClassAstSource create(@NonNull final Collection<File> sourceDirs,
      @NonNull final Collection<File> classpathElements) {
    return new CachingClassAstSource(new ProjectBuildConfigAstSource(sourceDirs, classpathElements));
  }
}
