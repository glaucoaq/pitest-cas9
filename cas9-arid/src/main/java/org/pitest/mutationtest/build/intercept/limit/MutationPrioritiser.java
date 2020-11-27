package org.pitest.mutationtest.build.intercept.limit;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparingInt;
import static lombok.AccessLevel.PRIVATE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.pitest.functional.FCollection;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator;
import org.pitest.mutationtest.engine.gregor.mutators.rv.AOR1Mutator;
import org.pitest.mutationtest.engine.gregor.mutators.rv.AOR2Mutator;
import org.pitest.mutationtest.engine.gregor.mutators.rv.AOR3Mutator;
import org.pitest.mutationtest.engine.gregor.mutators.rv.AOR4Mutator;
import org.pitest.mutationtest.engine.gregor.mutators.rv.ROR1Mutator;
import org.pitest.mutationtest.engine.gregor.mutators.rv.ROR2Mutator;
import org.pitest.mutationtest.engine.gregor.mutators.rv.ROR3Mutator;
import org.pitest.mutationtest.engine.gregor.mutators.rv.ROR4Mutator;
import org.pitest.mutationtest.engine.gregor.mutators.rv.ROR5Mutator;
import org.pitest.mutationtest.engine.gregor.mutators.rv.UOI1Mutator;
import org.pitest.mutationtest.engine.gregor.mutators.rv.UOI2Mutator;
import org.pitest.mutationtest.engine.gregor.mutators.rv.UOI3Mutator;
import org.pitest.mutationtest.engine.gregor.mutators.rv.UOI4Mutator;

@RequiredArgsConstructor(access = PRIVATE)
class MutationPrioritiser {

  private static final String PRIORITY_SEPARATOR = ",";

  private static final Map<String, List<String>> MUTATOR_PRIORITY_BY_OPERATOR = createMutatorPriority();

  private final List<String> mutationOrderedByPriority;

  static MutationPrioritiser of(String operatorPriority) {
    val operatorOrderedByPriority = new ArrayList<>(asList(operatorPriority.split(PRIORITY_SEPARATOR)));

    val mutationOrderedByPriority = MUTATOR_PRIORITY_BY_OPERATOR.entrySet().stream()
        .filter(entry -> operatorOrderedByPriority.contains(entry.getKey()))
        .sorted(comparingInt(entry -> operatorOrderedByPriority.indexOf(entry.getKey())))
        .flatMap(entry -> entry.getValue().stream())
        .collect(Collectors.toList());

    return new MutationPrioritiser(mutationOrderedByPriority);
  }

  Comparator<? super MutationDetails> makeMutatorPrioritiser() {
    return comparingInt(details ->
        mutationOrderedByPriority.contains(details.getMutator())
            ? mutationOrderedByPriority.indexOf(details.getMutator())
            : mutationOrderedByPriority.size());
  }

  private static Map<String, List<String>> createMutatorPriority() {
    val enginePackageName = "org.pitest.mutationtest.engine.cas9.mutators.sbr";
    val sbrMutator = enginePackageName + ".SBRMutator";
    val lcrMutator = enginePackageName + ".LCRMutator";
    Class<?>[] rorOrder =
        { ROR2Mutator.class, ROR4Mutator.class, ROR3Mutator.class, ROR5Mutator.class, ROR1Mutator.class };
    Class<?>[] aorOrder =
        { AOR1Mutator.class, AOR2Mutator.class, AOR3Mutator.class, AOR4Mutator.class };
    Class<?>[] uoiOrder =
        { UOI3Mutator.class, UOI4Mutator.class, UOI1Mutator.class, UOI2Mutator.class };

    Map<String, List<String>> byOperator = new HashMap<>();
    byOperator.put("ROR", FCollection.map(asList(rorOrder), Class::getName));
    byOperator.put("AOR", FCollection.map(asList(aorOrder), Class::getName));
    byOperator.put("UOI", FCollection.map(asList(uoiOrder), Class::getName));
    byOperator.put("SBR", asList(sbrMutator, VoidMethodCallMutator.class.getName()));
    byOperator.put("LCR", asList(lcrMutator + ".LCR1_MUTATOR", lcrMutator + ".LCR2_MUTATOR"));

    return byOperator;
  }
}
