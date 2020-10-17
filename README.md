# Cas9: A Mutation Engine for PIT

Cas9 is a [Pitest](https://github.com/hcoles/pitest) Mutation Engine with enhanced mutation targeting.

The filtering and context improvements of Cas9 are based on the mutation coverage capabilities of Google's Critique, as described in [State of Mutation Testing at Google](https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/46584.pdf).

## Quick start with Maven

To target a Maven project, you must add Pitest maven plugin and configure Cas9 as the mutation engine to use.

```xml
<plugin>
  <groupId>org.pitest</groupId>
  <artifactId>pitest-maven</artifactId>
  <version>${pitest.version}</version>
  <configuration>
    <mutationEngine>cas9</mutationEngine>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>com.github.glaucoaq</groupId>
      <artifactId>cas9-engine</artifactId>
      <version>${pitest-cas9.version}</version>
    </dependency>
    <!-- to add if you use JUnit 5: -->
    <dependency>
      <groupId>org.pitest</groupId>
      <artifactId>pitest-junit5-plugin</artifactId>
      <version>${pitest-junit5-plugin.version}</version>
    </dependency>
  </dependencies>
</plugin>
```

Then, execute the regular [mutation coverage goal](https://pitest.org/quickstart/maven/).

## Cas9 Engine Features

Cas9 improves [Gregor](https://github.com/hcoles/pitest/tree/master/pitest/src/main/java/org/pitest/mutationtest/engine/gregor) (the default mutation engine) by introducing new Features that support fine-grained mutation control.

When used as the mutation engine, Cas9 actives all its feature by default.

* `AST`: Parses the source code of the target class as an AST object.
* `LINELIMIT`: Limits the maximum number of mutations per line.
* `FCINCL`: Filters out mutations based on class-specific inclusion rules.
* `FCCOVL`: Filters out mutations in lines not covered by any unit test.
* `FSARID`: Filters out mutations based on predefined rules for _arid nodes_ detection.
* `FEARID`: Filters out mutations based on custom _expert rules_ for _arid nodes_ detection.

### Source Code Parsing

When the feature `AST` is enabled, the source code is parsed as a [JavaParser](https://javaparser.org/) AST that can be retrieved using an instance of `ClassAstSource`.

The parsing takes place before any mutator is executed and allows navigation between instructions and the corresponding AST node.

### Maximum Mutants per Line

The `LINELIMIT` feature limits the number of mutations in the same line of a given class. By default, for each line, at most one mutant is generated, which is picked by a deterministic but unpredictable logic from the set of applicable operators. This behavior conforms to Google's Critique, however, you can increase this number using the parameter `limit`.

```
+LINELIMIT(limit[3])
```

### Target Class Rules

If the feature `FCINCL` is enabled, Cas9 will look for a JSON resource file under a `root` (_cas9_ by default) directory, named using the FQN of the mutating class (e.g. `/cas9/com.company.product.MyClass.json`), and - if exists - load custom inclusion rules for lines.

To customize the location of the resource, use the `root` parameter of the feature:

```
+FCINCL(root[custom])
```

The contents of a class rules file must conform to the following schema:

```yaml
ranges:
  - first: m # an integer, e.g. 5
    last: k # an integer, e.g. 50
```

Currently, only range inclusion rules are supported. There must be at least one entry specifying the first and last line numbers, using consistent values (first > 0, first <= last and no overlapping ranges). Any mutation generated in such class that is not contained in a configured range is discarded.

#### Diff-based Analysis Support

Diff-based analysis is a key feature of Google's Critique:

> For each diff under review, mutation analysis is executed as soon as the incremental coverage analysis is complete. Incremental coverage analysis results are used to minimize the set of viable lines that should be mutated.

A code review tool can make use of the `FCINCL` to restrict the mutation analysis to the lines changed in a specific commit or pull request. Such tool must dynamically generate a class rules file with ranges accordingly to the Git diff and place it in a directory mapped in the execution classpath.

### Uncovered Lines Exclusion

Cas9 can prevent the generation of `NO_COVERAGE` mutations by discarding any mutants created in lines not covered by unit tests. If the feature `FCCOVL` is enabled, mutations are restricted to lines covered by unit tests.

### Arid Lines Exclusion

Arid lines are lines of code that are usually non relevant for unit testing. Likewise, covering mutations on such lines adds no value for quality and should be avoided for the sake of performance and simplicity. The suppression of arid lines cuts the number of potential, non relevant mutants and it's a key behavior of Google's Critique

The heuristic for classifying lines as _arid_ is two-fold:
- Simple statements are verified against an _expert function_ that defines rules for flagging lines as _arid_
- Compound statements (that have at least one body) are flagged as _arid_ if all of its statements are arid.

The function that flags simple nodes as arid has two types of rules:

- Global, predefined rules for commonly non-relevant mutations
- Global, mutation specific, replaceable custom functions.

#### Predefined Arid Node Expert Rules

The feature `FSARID` enables a set of predefined, global mutation exclusion rules.

The current version of Cas9 considers as arid lines:

- Calls to property accessors (trivial getters and setters);
- Calls to common methods in `java.lang.Object`, namely `equals`, `hashCode`, `toString`, and `clone`;
- Exception `throw` statements.

Mutations generated for such kind of lines are filtered out.

### Custom Arid Node Expert Rules

Unlike the predefined rules, the _expert function_ is replaceable: you can extend the default rules or define a completely new set of rules, that can be either global or mutator-specific.

To enable a custom expert function, use the `FEARID` feature, including the FQN of your custom function factory type:

```
+FEARID(expert[package.MyExpertFunctionFactory])
```

The factory type must be a `Supplier<Function<Statement, Boolean>>` (from [JavaParser](https://www.javadoc.io/doc/com.github.javaparser/javaparser-core/latest/com/github/javaparser/ast/stmt/Statement.html)).

Cas9 activates this feature by default, with the `DefaultExpertFunctionFactory` used.

## Cas9 Output

In addition to PIT reporting extensions, Cas9 and includes a JSON or XML report of arid nodes detection. The arid nodes report specifies the lines flagged as _arid_ and includes the class name and method signature, along with an option exclusion rule name. See an example below:

```yaml
com.company.project.MyClass:
  doIt(Ljava/lang/Object;)Z:
    1: OBJECT_METHOD
    8: GUICE_INJECTION
    # more lines ...
  makeIt()V:
    14: SYSTEM_METHOD
    # more lines...
  # more methods...
# more classes...
```

To generate the arid node report, use the ARID output format.

## Cas9 Default Configuration

Using Cas9 as mutation engine with no further configuration (all defaults) is equivalent to the following setup:

```xml
<plugin>
  <groupId>org.pitest</groupId>
  <artifactId>pitest-maven</artifactId>
  <version>${pitest.version}</version>
  <configuration>
    <mutationEngine>cas9</mutationEngine>
    <features>
      <feature>+AST</feature>
      <feature>+LINELIMIT(limit[1])</feature>
      <feature>+FCINCL(root[cas9])</feature>
      <feature>+FCCOVL</feature>
      <feature>+FSARID</feature>
      <feature>+FEARID(expert[org.pitest.mutationtest.filter.cas9.DefaultExpertFunctionFactory])</feature>
    </features>
    <outputFormats>
      <value>ARID</value>
    </outputFormats>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>com.github.glaucoaq</groupId>
      <artifactId>cas9-engine</artifactId>
      <version>${pitest-cas9.version}</version>
    </dependency>
  </dependencies>
</plugin>
```
