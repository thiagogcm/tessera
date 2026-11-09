package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.catchThrowableOfType;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.RenderContext;
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

  record Panel(String text) implements Component {
    @Override
    public Element render(RenderContext context) {
      return div().child(p().text(text));
    }
  }

  @Test
  @DisplayName("nothing is recorded or emitted unless tracking is on")
  void trackingIsOffByDefault() {
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
  }

  @Test
  @DisplayName("a user component's elements point into its own render method")
  void userComponentPointsIntoRender() {
    var page = SourceTracking.enabled(() -> RenderedPage.render(main().child(new Panel("hi"))));

    assertThat(page.element("p").attr("data-src")).startsWith(THIS_FILE + ":");
  }

  @Test
  @DisplayName("a library component's elements point at the line that placed the component")
  void libraryComponentPointsAtItsPlacement() {
    var page = SourceTracking.enabled(() -> RenderedPage.render(main().child(Button.of("Save"))));
    var line = here() - 1;

    assertThat(page.element("button").attr("data-src")).isEqualTo(THIS_FILE + ":" + line);
    assertThat(page.html()).doesNotContain("PageCompiler").doesNotContain("LayoutPass");
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
  }

  @Test
  @DisplayName("a diagnostic without tracking keeps its familiar form")
  void diagnosticsWithoutTrackingAreUnchanged() {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () -> Compiler.compile(new PageDocument("Bad", main().child(button()))));

    assertThat(failure.diagnostics().getFirst().location().source()).isNull();
    assertThat(failure.getMessage()).contains("<button>: Interactive <button>");
  }

  @Test
  @DisplayName("site compilation carries tracking onto its worker threads")
  void siteCompilationTracksComponentsRenderedOnWorkers() {
    var compiled =
        SourceTracking.enabled(
            () ->
                Compiler.compile(
                    SiteDefinition.builder()
                        .route(
                            new SiteRoute(
                                "/", new PageDocument("Home", main().child(new Panel("a")))))
                        .route(
                            new SiteRoute(
                                "/b", new PageDocument("B", main().child(new Panel("b")))))
                        .build()));

    assertThat(compiled.pages().get("/")).contains("data-src=\"" + THIS_FILE + ":");
    assertThat(compiled.pages().get("/b")).contains("data-src=\"" + THIS_FILE + ":");
  }

  @Test
  @DisplayName("site head content the compiler builds carries no source")
  void compilerBuiltHeadElementsHaveNoSource() {
    var compiled =
        SourceTracking.enabled(
            () ->
                Compiler.compile(
                    SiteDefinition.builder()
                        .route(new SiteRoute("/", new PageDocument("Home", main().text("Home"))))
                        .linkSpeculation(LinkSpeculation.PRERENDER)
                        .build()));

    var html = compiled.pages().get("/");
    assertThat(html)
        .contains("<script type=\"speculationrules\">")
        .contains("<main data-src=\"" + THIS_FILE + ":");
  }

  @Test
  @DisplayName("the page tree lists elements with their components and call sites")
  void treeShowsComponentsAndSources() {
    var tree =
        SourceTracking.enabled(
            () -> Compiler.explain(new PageDocument("Tree", main().child(new Panel("hi")))));
    var text = tree.toText();

    assertThat(text).contains("<main>").contains("<p>").contains("\"hi\"").contains("in Panel");
    assertThat(text).containsPattern("<p>.*at " + THIS_FILE + ":\\d+");

    var body = (PageTree.Tag) tree.body().getFirst();
    assertThat(body.tag()).isEqualTo("main");
    assertThat(body.children()).hasSize(1);
  }

  private static SourceLocation sourceAt(int line) {
    return new SourceLocation(THIS_FILE, line);
  }
}
