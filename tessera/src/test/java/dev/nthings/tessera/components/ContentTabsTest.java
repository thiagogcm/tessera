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
import dev.nthings.tessera.components.ContentTabs.Activation;
import java.util.List;
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
        .containsEntry("aria-controls", "stages--panel-java")
        .containsEntry("tabindex", "0");
    assertThat(find(tree, "stages--panel-java").attributes())
        .containsEntry("role", "tabpanel")
        .containsEntry("aria-labelledby", "stages--tab-java")
        .doesNotContainKey("hidden");
    assertThat(find(tree, "stages--panel-html").attributes())
        .containsEntry("role", "tabpanel")
        .containsEntry("aria-labelledby", "stages--tab-html")
        .containsEntry("hidden", "");
    assertThat(find(tree, "stages--tab-html").attributes())
        .containsEntry("aria-selected", "false")
        .containsEntry("tabindex", "-1");

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
  @DisplayName("static tabs expose an explicit selection and manual activation")
  void explicitSelectionAndActivation() {
    var tabs =
        ContentTabs.of("Stages")
            .id("stages")
            .activation(Activation.MANUAL)
            .disabledTab("java", "Java", p().text("Builders"))
            .tab("html", "HTML", p().text("Markup"))
            .tab("css", "CSS", p().text("Styles"))
            .initialSelected("css");
    var tree = Compiler.explain(new PageDocument("Tabs", main().child(tabs)));
    assertThat(findAttribute(tree, "data-scope-id", "stages").attributes())
        .containsEntry("data-activation", "manual");
    assertThat(find(tree, "stages--tab-java").attributes())
        .containsEntry("disabled", "")
        .containsEntry("aria-disabled", "true")
        .containsEntry("tabindex", "-1");
    assertThat(find(tree, "stages--tab-css").attributes()).containsEntry("aria-selected", "true");

    tabs.initialSelected("java");
    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(() -> Compiler.explain(new PageDocument("Tabs", main().child(tabs))))
        .withMessageContaining("tab-selection");
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
    if (found == null) {
      throw new AssertionError("No element has id: " + id);
    }
    return found;
  }

  private static PageTree.Tag findAttribute(PageTree tree, String name, String value) {
    var found = find(tree.body(), tag -> value.equals(tag.attributes().get(name)));
    if (found == null) {
      throw new AssertionError("No element has " + name + "=" + value);
    }
    return found;
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
