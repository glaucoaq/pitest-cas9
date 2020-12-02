package org.pitest.ast;

import static com.google.common.collect.Iterables.getLast;
import static java.util.Arrays.asList;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.pitest.classinfo.ClassName;

class ProjectBuildConfigAstSource implements ClassAstSource {

  @SuppressWarnings("unchecked")
  private static final Class<TypeDeclaration<?>> TYPE_DECLR_CLASS =
      (Class<TypeDeclaration<?>>) (Class<?>) TypeDeclaration.class;

  private final JavaParser parser;

  private final BiFunction<String, String, Optional<Reader>> sourceLocator;

  ProjectBuildConfigAstSource(@NonNull final Collection<File> sourceDirs,
      @NonNull final Collection<File> classPathElements) {
    this.parser = createParser(sourceDirs, classPathElements);
    val locators = sourceDirs.stream()
        .map(ProjectBuildConfigAstSource::createLocator)
        .collect(Collectors.toList());
    this.sourceLocator = (className, fileName) -> locators.stream()
        .flatMap(locator -> locator.apply(className, fileName))
        .findFirst();
  }

  @Override
  public Optional<TypeDeclaration<?>> getAst(ClassName className, String fileName) {
    val fullName = className.asJavaName();
    val javaName = className
        .getNameWithoutPackage()
        .asJavaName();
    val simpleName = javaName.contains("$") ? getLast(asList(javaName.split("\\$"))) : javaName;

    Predicate<TypeDeclaration<?>> hasSimpleName = type -> type.getNameAsString().equals(simpleName);

    return sourceLocator.apply(fullName, fileName)
        .map(parser::parse)
        .map(result -> result.getResult()
            .orElseThrow(() -> new ParseProblemException(result.getProblems())))
        .flatMap(unit -> unit.findFirst(TYPE_DECLR_CLASS, hasSimpleName));
  }

  private static JavaParser createParser(@NonNull Collection<File> sourceDirs,
      @NonNull Collection<File> classPathElements) {
    val reflectionSolver = new ReflectionTypeSolver();

    val dependencyUrls = classPathElements.stream()
        .map(ProjectBuildConfigAstSource::toUrl)
        .toArray(URL[]::new);
    val dependencySolver = new ClassLoaderTypeSolver(new URLClassLoader(dependencyUrls));

    val sourceDirSolvers = sourceDirs.stream()
        .map(JavaParserTypeSolver::new)
        .toArray(TypeSolver[]::new);
    val sourcesSolver = sourceDirSolvers.length == 1 ? sourceDirSolvers[0] : new CombinedTypeSolver(sourceDirSolvers);

    val typeSolver = new CombinedTypeSolver(reflectionSolver, dependencySolver, sourcesSolver);
    val symbolSolver = new JavaSymbolSolver(typeSolver);
    val configuration = new ParserConfiguration().setSymbolResolver(symbolSolver);

    return new JavaParser(configuration);
  }

  @SneakyThrows
  private static URL toUrl(File file) {
    return file.toURI().toURL();
  }

  private static BiFunction<String, String, Stream<Reader>> createLocator(@NonNull final File rootDirectory) {
    val rootPath = rootDirectory.toPath();
    return (className, fileName) -> Stream.of(
        rootPath.resolve(
            className.contains(".")
                ? Paths.get(className.replace(".", "/"), "..", fileName)
                : Paths.get(fileName))
            .normalize())
        .filter(Files::exists)
        .map(path -> {
          try {
            return Files.newBufferedReader(path);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
  }
}
