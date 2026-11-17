package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.catchThrowableOfType;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.SourceLocation;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Source provenance")
class SourceProvenanceTest {

  private static final String THIS_FILE = "SourceProvenanceTest.java";

  private static int here() {
    return new Throwable().getStackTrace()[1].getLineNumber();
  }

  static final class Panel implements Component {
    private final String text;
    private int elementLine;

    Panel(String text) {
      this.text = text;
    }

    @Override
    public Element expand(DocumentContext context) {
      elementLine = here() + 1;
      return div().child(p().text(text));
    }
  }

  @Test
  @DisplayName("records and emits no source by default")
  void recordsNothingByDefault() {
    var tag = div();
    var page = RenderedPage.render(main().child(tag));

    assertThat(tag.source()).isNull();
    assertThat(page.html()).doesNotContain("data-src");
  }

  @Test
  @DisplayName("an element records the line that built it and emits it as data-src")
  void elementCarriesItsCallSite() {
    var built = SourceTracking.enabled(() -> div().text("hello"));
    var line = here() - 1;

    assertThat(built.source()).isEqualTo(sourceAt(line));

    var page = RenderedPage.render(main().child(built));
    assertThat(page.element("div").attr("data-src")).isEqualTo(THIS_FILE + ":" + line);
    assertNoCompilerSources(page.html());
  }

  @Test
  @DisplayName("a user component's elements point into the component's own code")
  void userComponentKeepsSource() {
    var panel = new Panel("hi");
    var page = SourceTracking.enabled(() -> RenderedPage.render(main().child(panel)));

    assertThat(page.element("p").attr("data-src")).isEqualTo(THIS_FILE + ":" + panel.elementLine);
    assertNoCompilerSources(page.html());
  }

  @Test
  @DisplayName("a library component's elements point at the line that placed the component")
  void libraryComponentKeepsPlacement() {
    var page = SourceTracking.enabled(() -> RenderedPage.render(main().child(Button.of("Save"))));
    var line = here() - 1;

    assertThat(page.element("button").attr("data-src")).isEqualTo(THIS_FILE + ":" + line);
    assertNoCompilerSources(page.html());
  }

  @Test
  @DisplayName("a diagnostic names the line of the offending element")
  void diagnosticsNameTheSourceLine() {
    var page = SourceTracking.enabled(() -> new PageDocument("Bad", main().child(button())));
    var line = here() - 1;

    var failure = catchThrowableOfType(CompilationException.class, () -> Compiler.compile(page));

    var diagnostic = failure.diagnostics().getFirst();
    assertThat(diagnostic.location().source()).isEqualTo(sourceAt(line));
    assertThat(failure.getMessage()).contains("<button> (" + THIS_FILE + ":" + line + "): ");
    assertNoCompilerSources(failure.getMessage());
  }

  @Test
  @DisplayName("a diagnostic without tracking omits its source location")
  void diagnosticsOmitSource() {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () -> Compiler.compile(new PageDocument("Bad", main().child(button()))));

    assertThat(failure.diagnostics().getFirst().location().source()).isNull();
    assertThat(failure.getMessage())
        .contains("<button>: Interactive <button>")
        .doesNotContain(THIS_FILE + ":");
  }

  @Test
  @DisplayName("site compilation tracks pages rendered on worker threads")
  void tracksSiteWorkerThreads() {
    var first = new Panel("a");
    var second = new Panel("b");
    var compiled =
        SourceTracking.enabled(() ->
            Compiler.compile(
                SiteDefinition.builder()
                    .route(new SiteRoute("/", new PageDocument("Home", main().child(first))))
                    .route(new SiteRoute("/b", new PageDocument("B", main().child(second))))
                    .build()));

    assertThat(compiled.pages().get("/"))
        .contains("data-src=\"" + THIS_FILE + ":" + first.elementLine + "\"");
    assertThat(compiled.pages().get("/b"))
        .contains("data-src=\"" + THIS_FILE + ":" + second.elementLine + "\"");
    assertNoCompilerSources(compiled.pages().get("/"));
    assertNoCompilerSources(compiled.pages().get("/b"));
  }

  @Test
  @DisplayName("compiler-built head content carries no source")
  void headElementsHaveNoSource() {
    var mainLine = new int[1];
    var compiled =
        SourceTracking.enabled(() -> {
          var body = main().text("Home");
          mainLine[0] = here() - 1;
          return Compiler.compile(
              SiteDefinition.builder()
                  .route(new SiteRoute("/", new PageDocument("Home", body)))
                  .linkSpeculation(LinkSpeculation.PRERENDER)
                  .build());
        });

    var page = RenderedPage.of(compiled, "/");
    assertThat(page.elements("head [data-src]")).isEmpty();
    assertThat(page.element("head > script[type=speculationrules]"))
        .doesNotHaveAttribute("data-src");
    assertThat(page.element("main").attr("data-src")).isEqualTo(THIS_FILE + ":" + mainLine[0]);
    assertNoCompilerSources(page.html());
  }

  @Test
  @DisplayName("the page tree lists elements with their components and call sites")
  void treeShowsComponentsAndSources() {
    var panel = new Panel("hi");
    var tree =
        SourceTracking.enabled(
            () -> Compiler.explain(new PageDocument("Tree", main().child(panel))));
    var text = tree.toText();

    assertThat(text).contains("<main>").contains("<p>").contains("\"hi\"").contains("in Panel");
    assertThat(text).contains("at " + THIS_FILE + ":" + panel.elementLine);
    assertNoCompilerSources(text);

    var body = (PageTree.Tag) tree.body().getFirst();
    assertThat(body.tag()).isEqualTo("main");
    assertThat(body.children()).hasSize(1);
    var paragraph = (PageTree.Tag) body.children().getFirst();
    assertThat(paragraph.source()).isEqualTo(sourceAt(panel.elementLine));
  }

  private static SourceLocation sourceAt(int line) {
    return new SourceLocation(THIS_FILE, line);
  }

  private static void assertNoCompilerSources(String text) {
    assertThat(text)
        .doesNotContain(
            "PageCompiler.java",
            "DocumentPass.java",
            "SourceTracking.java",
            "SourceProvenance.java",
            "HtmlWriter.java",
            "PageValidator.java",
            "SiteCompiler.java",
            "Expansion.java",
            "ExpansionState.java",
            "DocumentState.java",
            "CompilationState.java",
            "ReferenceLinker.java",
            "dev.nthings.tessera.compiler");
  }
}
