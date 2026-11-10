package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import dev.nthings.tessera.compiler.CompilationException;
import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.compiler.PageTree;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ContentTabs")
class ContentTabsTest {

  @Test
  @DisplayName("links each tab to its panel by author key and selects the first")
  void linksTabsToPanels() {
    var document =
        new PageDocument(
            "Tabs",
            main()
                .child(
                    ContentTabs.of("Stages")
                        .id("stages")
                        .tab("java", "Java", p().text("Builders"))
                        .tab("html", "HTML", p().text("Markup"))));
    var tree = Compiler.explain(document);

    var root = findAttribute(tree, "data-scope-id", "stages");
    assertThat(root.tag()).isEqualTo("tessera-tabs");
    assertThat(root.scope()).startsWith("content-tabs-");
    assertThat(findAttribute(tree, "role", "tablist").attributes())
        .containsEntry("aria-label", "Stages");
    assertThat(find(tree, "stages--tab-java").attributes())
        .containsEntry("role", "tab")
        .containsEntry("aria-selected", "true")
        .containsEntry("aria-controls", "stages--panel-java");
    assertThat(find(tree, "stages--panel-html").attributes())
        .containsEntry("role", "tabpanel")
        .containsEntry("aria-labelledby", "stages--tab-html")
        .containsEntry("hidden", "");
    assertThat(find(tree, "stages--tab-html").attributes()).containsEntry("aria-selected", "false");

    var encoded =
        Compiler.compile(
            new PageDocument(
                "Encoded",
                main()
                    .child(
                        ContentTabs.of("A & B")
                            .id("encoded")
                            .tab("one", "Left & right", p().text("x")))));
    assertThat(encoded.html()).contains("aria-label=\"A &amp; B\"").contains("Left &amp; right");
  }

  @Test
  @DisplayName("a blank tab key is rejected")
  void blankTabKeyIsRejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ContentTabs.of("Stages").tab(" ", "Blank", p().text("x")))
        .withMessage("A tab key must not be blank");
  }

  @Test
  @DisplayName("a repeated tab key is reported")
  void reportsDuplicateTabKey() {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () ->
                Compiler.compile(
                    new PageDocument(
                        "Tabs",
                        main()
                            .child(
                                ContentTabs.of("Stages")
                                    .tab("java", "Java", p().text("One"))
                                    .tab("java", "Again", p().text("Two"))))));

    assertThat(failure.diagnostics())
        .filteredOn(diagnostic -> diagnostic.rule().equals("duplicate-tab"))
        .singleElement()
        .satisfies(diagnostic -> assertThat(diagnostic.message()).contains("java"));
  }

  @Test
  @DisplayName("empty content tabs are reported")
  void reportsEmptyTabs() {
    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(
            () -> Compiler.compile(new PageDocument("Tabs", main().child(ContentTabs.of("Empty")))))
        .withMessageContaining("empty-content-tabs")
        .withMessageContaining("tab(key, label, content)");
  }

  private static PageTree.Tag find(PageTree tree, String id) {
    var found = find(tree.body(), tag -> id.equals(tag.id()));
    assertThat(found).as("element %s", id).isNotNull();
    return Objects.requireNonNull(found);
  }

  private static PageTree.Tag findAttribute(PageTree tree, String name, String value) {
    var found = find(tree.body(), tag -> value.equals(tag.attributes().get(name)));
    assertThat(found).as("%s=%s", name, value).isNotNull();
    return Objects.requireNonNull(found);
  }

  private static PageTree.Tag find(List<PageTree.Node> nodes, Predicate<PageTree.Tag> match) {
    for (var node : nodes) {
      if (node instanceof PageTree.Tag tag) {
        if (match.test(tag)) {
          return tag;
        }
        var nested = find(tag.children(), match);
        if (nested != null) {
          return nested;
        }
      }
    }
    return null;
  }
}
