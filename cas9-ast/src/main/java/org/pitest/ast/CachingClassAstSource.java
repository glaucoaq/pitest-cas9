package org.pitest.ast;

import com.github.javaparser.ast.body.TypeDeclaration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.Value;
import lombok.val;
import org.pitest.classinfo.ClassName;

@Value
class CachingClassAstSource implements ClassAstSource {

  private static final int MAX_ENTRIES = 200;

  Map<String, TypeDeclaration<?>> cache = new LinkedHashMap<String, TypeDeclaration<?>>() {
    @Override
    protected boolean removeEldestEntry(Entry eldest) {
      return size() > MAX_ENTRIES;
    }
  };

  ClassAstSource child;

  @Override
  public Optional<TypeDeclaration<?>> getAst(ClassName className, String fileName) {
    val key = className.asJavaName();
    if (!cache.containsKey(key)) {
      child.getAst(key, fileName)
          .ifPresent(type -> cache.put(key, type));
    }
    return Optional.ofNullable(cache.get(key));
  }
}
