package org.pitest.mutationtest.build.intercept.arid;

import static lombok.AccessLevel.PACKAGE;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.pitest.classpath.ClassPath;
import org.pitest.mutationtest.arid.AridityDetectionManagerFactory;
import org.pitest.mutationtest.arid.ranges.LineRangeAridityDetectionManagerFactory;
import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.build.MutationInterceptorFactory;
import org.pitest.plugin.Feature;
import org.pitest.plugin.FeatureParameter;

@RequiredArgsConstructor(access = PACKAGE)
public class LineRangeAridNodeMutationFilterFactory  implements MutationInterceptorFactory {

  static final String FEATURE_NAME = "FLRANGE";

  static final FeatureParameter ROOT_PARAM = FeatureParameter
      .named("root")
      .withDescription("Name of the root folder of class line range config resources");

  private static final Path DEFAULT_ROOT_PATH = Paths.get("/cas9/");

  private final BiFunction<Path, ClassPath, AridityDetectionManagerFactory> createFactory;

  @SuppressWarnings("unused")
  public LineRangeAridNodeMutationFilterFactory() {
    this(LineRangeAridityDetectionManagerFactory::of);
  }

  @Override
  public MutationInterceptor createInterceptor(InterceptorParameters params) {
    val classPath = params.data().getClassPath();
    val rootPath = params.getString(ROOT_PARAM)
        .map(root -> "/" + root + "/")
        .map(Paths::get)
        .orElse(DEFAULT_ROOT_PATH);
    val factory = createFactory.apply(rootPath, classPath);
    return new AridNodeMutationFilter(factory);
  }

  @Override
  public Feature provides() {
    return Feature
        .named(FEATURE_NAME)
        .withOnByDefault(false)
        .withDescription("Filters out mutations based on class-level configured line ranges");
  }

  @Override
  public String description() {
    return "Arid nodes line range configuration filter";
  }
}
