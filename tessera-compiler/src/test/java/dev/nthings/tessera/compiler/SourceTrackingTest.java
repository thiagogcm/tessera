package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.document.Documents.rawHtml;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.catchThrowableOfType;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.SourceLocation;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Source tracking")
class SourceTrackingTest {

  private static final String THIS_FILE = "SourceTrackingTest.java";

  private static int here() {
    return new Throwable().getStackTrace()[1].getLineNumber();
  }

  private static SourceLocation sourceAt(int line) {
    return new SourceLocation(THIS_FILE, line);
  }

  static final class Panel implements Component {
    private final String text;
    private int elementLine;

    Panel(String text) {
      this.text = text;
    }

    @Override
    public Element expand(ViewContext context) {
      elementLine = here() + 1;
      return div().child(p().text(text));
    }
  }

  /// Every emitted source points into this test, never into the compiler or the library.
  private static void assertSourcesPointHere(RenderedPage page) {
    assertThat(page.elements("[data-src]"))
        .isNotEmpty()
        .allSatisfy(element -> assertThat(element.attr("data-src")).startsWith(THIS_FILE + ":"));
  }

  @Test
  @DisplayName("records and emits no source by default")
  void recordsNothingByDefault() {
    var page = RenderedPage.renderMain(div());
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () -> Compiler.forPage().compile(PageDocument.of("Bad", main(button()))));

    assertThat(page.elements("[data-src]")).isEmpty();
    assertThat(failure.diagnostics().getFirst().location().source()).isNull();
  }

  @Test
  @DisplayName("an element records the line that built it and emits it as data-src")
  void elementCarriesItsCallSite() {
    var built = SourceTracking.run(true, () -> div().text("hello"));
    var line = here() - 1;

    var page = RenderedPage.renderMain(built);

    assertThat(page.element("div")).hasAttribute("data-src", sourceAt(line).toString());
    assertSourcesPointHere(page);
  }

  @Test
  @DisplayName("a user component's elements point into the component's own code")
  void userComponentKeepsSource() {
    var panel = new Panel("hi");
    var page = SourceTracking.run(true, () -> RenderedPage.render(main().child(panel)));

    assertThat(page.element("p")).hasAttribute("data-src", sourceAt(panel.elementLine).toString());
    assertSourcesPointHere(page);
  }

  @Test
  @DisplayName("a library component's elements point at the line that placed the component")
  void libraryComponentKeepsPlacement() {
    var page = SourceTracking.run(true, () -> RenderedPage.render(main().child(Button.of("Save"))));
    var line = here() - 1;

    assertThat(page.element("button")).hasAttribute("data-src", sourceAt(line).toString());
    assertSourcesPointHere(page);
  }

  @Test
  @DisplayName("a diagnostic is located at the line of the offending element")
  void diagnosticsNameTheSourceLine() {
    var page = SourceTracking.run(true, () -> PageDocument.of("Bad", main(button())));
    var line = here() - 1;

    var failure =
        catchThrowableOfType(CompilationException.class, () -> Compiler.forPage().compile(page));

    assertThat(failure.diagnostics().getFirst().location().source()).isEqualTo(sourceAt(line));
  }

  @Test
  @DisplayName("site pages rendered on worker threads keep their sources; head content has none")
  void tracksSiteWorkerThreads() {
    var first = new Panel("a");
    var second = new Panel("b");
    var compiled =
        SourceTracking.run(
            true,
            () ->
                Compiler.forSite()
                    .compile(
                        SiteDefinition.of(
                            SiteRoute.of("/", PageDocument.of("Home", main(first))),
                            SiteRoute.of("/b", PageDocument.of("B", main(second))))));

    var home = RenderedPage.of(compiled, "/");
    var other = RenderedPage.of(compiled, "/b");
    assertThat(home.element("p")).hasAttribute("data-src", sourceAt(first.elementLine).toString());
    assertThat(other.element("p"))
        .hasAttribute("data-src", sourceAt(second.elementLine).toString());
    assertThat(home.elements("head [data-src]")).isEmpty();
    assertSourcesPointHere(home);
    assertSourcesPointHere(other);
  }

  @Test
  @DisplayName("the page tree lists elements with their components, call sites and raw HTML")
  void explainsThePageTree() {
    var panel = new Panel("hi");
    var svg = "<svg viewBox=\"0 0 10 10\"><circle cx=\"5\" cy=\"5\" r=\"4\"/></svg>";
    var tree =
        SourceTracking.run(
            true,
            () ->
                Compiler.explain(
                    PageDocument.of(
                        "Tree", DocumentElement.of(main()).child(panel).child(rawHtml(svg)))));

    var body = (PageTree.Tag) tree.body().getFirst();
    assertThat(body.tag()).isEqualTo("main");
    assertThat(body.children()).hasSize(2);
    var paragraph = ((PageTree.Tag) body.children().getFirst()).children().getFirst();
    assertThat(paragraph)
        .isInstanceOfSatisfying(
            PageTree.Tag.class,
            tag -> {
              assertThat(tag.tag()).isEqualTo("p");
              assertThat(tag.componentPath()).containsExactly("Panel");
              assertThat(tag.source()).isEqualTo(sourceAt(panel.elementLine));
              assertThat(tag.children()).containsExactly(new PageTree.Text("hi"));
            });
    assertThat(body.children().get(1)).isEqualTo(new PageTree.Raw(svg.length()));
  }
}
