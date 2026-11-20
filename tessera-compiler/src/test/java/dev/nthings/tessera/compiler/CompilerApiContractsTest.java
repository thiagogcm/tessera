package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static org.assertj.core.api.Assertions.assertThat;

import dev.nthings.tessera.components.Section;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssDuration;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.Documents;
import dev.nthings.tessera.document.components.DocLayout;
import dev.nthings.tessera.document.components.Footnotes;
import dev.nthings.tessera.document.components.TableOfContents;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Compiler and document public API contracts")
class CompilerApiContractsTest {

  @Test
  @DisplayName("built-in document components do not expose expand publicly")
  void builtInDocumentComponentsHideExpand() {
    for (var type : List.<Class<?>>of(DocLayout.class, Footnotes.class, TableOfContents.class)) {
      var publicExpand =
          Arrays.stream(type.getMethods())
              .filter(m -> m.getName().equals("expand") && Modifier.isPublic(m.getModifiers()))
              .toList();
      assertThat(publicExpand).as(type.getSimpleName()).isEmpty();
    }
  }

  @Test
  @DisplayName(
      "DocLayout accepts varargs, portable and stream children, and Documents.fragment accepts streams")
  void documentBuildersSupportSymmetricOverloads() {
    var layout =
        DocLayout.of()
            .children(Section.of("one", "One").child(p().text("First")))
            .children(
                DocumentContent.of(Section.of("two", "Two").child(p().text("Second"))),
                Documents.fragment(Stream.of(DocumentContent.of(p().text("Fragment item")))))
            .children(Stream.of(DocumentContent.of(Section.of("three", "Three"))));

    var page = Compiler.forPage().compile(PageDocument.of("Docs", layout));
    assertThat(page.html()).contains("First", "Second", "Fragment item", "Three");
  }

  @Test
  @DisplayName("Compiler configures Theme and OutputMode, and OutputMode is a sealed value type")
  void compilerConfigurationAndOutputModeSemantics() {
    assertThat(OutputMode.class.isSealed()).isTrue();
    assertThat(OutputMode.external("/assets/")).isEqualTo(OutputMode.external("/assets"));
    assertThat(OutputMode.inline()).isNotEqualTo(OutputMode.external("/assets"));
    assertThat(CssDuration.of(Duration.ofMillis(250))).isEqualTo(CssDuration.ms(250));

    var theme = Theme.defaults().with(Tokens.ACCENT_PRIMARY, CssColor.oklch(55, 0.18, 150));
    var doc =
        PageDocument.of("Overloads", (IntoElement) main(p("Hi")))
            .withLang(Locale.forLanguageTag("pt-BR"));

    var themed = Compiler.forPage().withTheme(theme).compile(doc);
    assertThat(themed.html()).contains("lang=\"pt-BR\"");
    assertThat(themed.css()).contains("oklch(55% 0.18 150)");
    assertThat(themed.assets()).isEmpty();

    var external = Compiler.forPage().withOutputMode(OutputMode.external("/static")).compile(doc);
    assertThat(external.assets()).hasSize(1);
    assertThat(external.assets().getFirst().path()).startsWith("/static/styles-");

    var ok = Compiler.forPage().tryCompile(doc);
    assertThat(ok).isInstanceOf(CompilationResult.Compiled.class);
    assertThat(ok.orThrow().html()).contains("Hi");

    var failed = Compiler.forPage().tryCompile(PageDocument.of("Bad", main(Elements.button())));
    switch (failed) {
      case CompilationResult.Compiled<CompiledPage> _ ->
          throw new AssertionError("Expected failure");
      case CompilationResult.Failed<CompiledPage>(var diagnostics) ->
          assertThat(diagnostics).isNotEmpty();
    }
  }

  @Test
  @DisplayName("PageTree.Tag defensively copies its attributes map")
  void pageTreeTagDefensivelyCopiesAttributes() {
    var attrs = new LinkedHashMap<String, String>();
    attrs.put("id", "hero");
    var tag = new PageTree.Tag("div", List.of(), null, attrs, List.of());

    attrs.put("id", "mutated");
    attrs.put("data-extra", "leak");

    assertThat(tag.attributes()).containsExactlyEntriesOf(java.util.Map.of("id", "hero"));
  }

  @Test
  @DisplayName("SiteRoute.normalizePath normalizes paths consistently")
  void siteRouteNormalizePath() {
    assertThat(SiteRoute.normalizePath("guide/")).isEqualTo("/guide");
    assertThat(SiteRoute.normalizePath(" / ")).isEqualTo("/");
    assertThat(SiteRoute.normalizePath("/docs/index.html")).isEqualTo("/docs/index.html");
  }
}
