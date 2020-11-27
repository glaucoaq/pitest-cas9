package org.pitest.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import java.nio.file.Paths;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.engine.Location;
import org.pitest.mutationtest.engine.MethodName;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;

class ClassAstSourceTest {

  private static final String SOURCE_FILE_NAME = "/ast/Example.java";

  private static final String TARGET_CLASS_NAME = "org.pitest.ast.test.Example";

  private static final int TARGET_LINE_NUMBER = 12;

  private static final String TARGET_COMMENT_LINE = "ClassAstSourceTest.TARGET_LINE_NUMBER";

  @Test
  void getAstNodeShouldFindNodeByLineNumberWithExpectedComment() throws Exception {
    // Arrange
    val sourceFileUrl = ClassAstSourceTest.class.getResource(SOURCE_FILE_NAME);
    val filePath = Paths.get(sourceFileUrl.toURI());
    val unit = StaticJavaParser.parse(filePath);

    val location = new Location(ClassName.fromString(TARGET_CLASS_NAME), MethodName.fromString("doIt"), "V()");
    val identity = new MutationIdentifier(location, 0, "MUTATOR");
    val details = new MutationDetails(identity, "Example.java", "", TARGET_LINE_NUMBER, 0);

    ClassAstSource astSource = (className, fileName) -> unit.getPrimaryType();

    // Act
    val actual = astSource.getAstNode(details).orElse(new EmptyStmt());

    // Assert
    val commentLine = actual.getComment()
        .map(Comment::getContent)
        .map(String::trim)
        .orElse("");

    assertAll(
        () -> assertThat(actual, instanceOf(ExpressionStmt.class)),
        () -> assertThat(commentLine, is(TARGET_COMMENT_LINE))
    );
  }
}
