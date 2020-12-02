package org.pitest.mutationtest.build.intercept.ast;

import java.io.File;
import java.util.Collection;
import lombok.val;
import org.pitest.bytecode.analysis.ClassTree;
import org.pitest.functional.FCollection;
import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.mutationtest.build.InterceptorType;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.build.MutationInterceptorFactory;
import org.pitest.mutationtest.config.EnvironmentAstContext;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.plugin.Feature;

public class ClassAstSettingsInterceptorFactory implements MutationInterceptorFactory {

  public static final String FEATURE_NAME = "AST";

  @Override
  public MutationInterceptor createInterceptor(InterceptorParameters params) {
    val options = params.data();
    val sourceDirs = options.getSourceDirs();
    val classpathElements = FCollection.map(options.getClassPathElements(), File::new);
    EnvironmentAstContext.saveSettings(sourceDirs, classpathElements);
    return new ClassAstSettingsInterceptor();
  }

  @Override
  public Feature provides() {
    return Feature.named(FEATURE_NAME)
        .withOnByDefault(true)
        .withDescription("Parses the source code of the target class as an AST object");
  }

  @Override
  public String description() {
    return "Source code AST provider plugin";
  }

  private final static class ClassAstSettingsInterceptor implements MutationInterceptor {

    @Override
    public InterceptorType type() {
      return InterceptorType.OTHER;
    }

    @Override
    public void begin(ClassTree clazz) { }

    @Override
    public Collection<MutationDetails> intercept(Collection<MutationDetails> mutations, Mutater m) {
      return mutations;
    }

    @Override
    public void end() { }
  }
}
