package org.pitest.mutationtest.engine.cas9.mutators.sbr;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;
import static org.pitest.functional.FCollection.contains;
import static org.pitest.functional.prelude.Prelude.not;

import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import lombok.NonNull;
import lombok.val;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.cas9.AstNodeTracker;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.MethodVisitor;

class SBRMethodVisitor extends AbstractInstructionRemovalMethodVisitor {

  private static final Collection<Class<? extends Statement>> BLOCK_TYPES = unmodifiableCollection(asList(
      DoStmt.class, ForEachStmt.class, ForStmt.class, IfStmt.class, SwitchStmt.class, WhileStmt.class));

  private final MethodMutatorFactory factory;

  private final MutationContext context;

  private final AstNodeTracker tracker;

  private final Map<Integer, MutationIdentifier> mutationByLine = new HashMap<>();

  public SBRMethodVisitor(@NonNull MethodMutatorFactory factory, @NonNull MutationContext context,
      @NonNull AstNodeTracker tracker, @NonNull MethodVisitor visitor) {
    super(visitor);
    this.factory = factory;
    this.context = context;
    this.tracker = tracker;
  }

  @Override
  protected boolean shouldPreserveInsn(int index) {
    return !createBlockRemovalMutation();
  }

  // TODO: improve name + cache block likes for each line
  private boolean createBlockRemovalMutation() {
    val blockLines = tracker.getCurrentStatement()
        .map(SBRMethodVisitor::findBlockLines)
        .orElse(emptyList());

    blockLines.stream()
        .findFirst()
        .filter(not(mutationByLine::containsKey))
        .ifPresent(line -> mutationByLine.put(line,
            context.registerMutation(factory, "removed block starting at line " + line)));

    return blockLines.stream()
        .filter(mutationByLine::containsKey)
        .map(mutationByLine::get)
        .anyMatch(context::shouldMutate);
  }

  private static Collection<Integer> findBlockLines(final Statement stmt) {
    return findBlockLines(stmt, new LinkedList<>());
  }

  private static Collection<Integer> findBlockLines(final Statement stmt, Collection<Integer> blocks) {
    if (contains(BLOCK_TYPES, type -> type.isInstance(stmt))) {
      stmt.getRange()
          .map(range -> range.begin.line)
          .ifPresent(blocks::add);
    }
    return stmt.findAncestor(Statement.class)
        .map(parent -> findBlockLines(parent, blocks))
        .orElse(blocks);
  }
}
