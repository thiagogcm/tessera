package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.main;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FontFace")
class FontFaceTest {

  private static final Asset INTER = Asset.of("/fonts/inter.woff2", new byte[] {1, 2, 3});
  private static final Asset INTER_ITALIC =
      Asset.of("/fonts/inter-italic.woff2", new byte[] {4, 5, 6});

  private static CompiledSite site(OutputMode output) {
    return Compiler.forSite()
        .withOutputMode(output)
        .compile(
            SiteDefinition.of(
                    SiteRoute.of("/", PageDocument.of("Home", main().text("Home"))),
                    SiteRoute.of("/about", PageDocument.of("About", main().text("About"))))
                .withFonts(
                    List.of(
                        FontFace.of("Inter", INTER).weight(100, 900).preload(),
                        FontFace.of("Inter", INTER_ITALIC).weight(400).italic())));
  }

  @Test
  @DisplayName("the stylesheet declares each face with swap display and the file's format")
  void declaresFaces() {
    assertThat(site(OutputMode.external("/assets")).css())
        .startsWith(
            "@font-face{font-family:\"Inter\";src:url(\"/fonts/inter.woff2\") format(\"woff2\");"
                + "font-weight:100 900;font-style:normal;font-display:swap}"
                + "@font-face{font-family:\"Inter\";src:url(\"/fonts/inter-italic.woff2\")"
                + " format(\"woff2\");font-weight:400;font-style:italic;font-display:swap}");
  }

  @Test
  @DisplayName("the site publishes each font file at its path")
  void publishesFiles() {
    var files = site(OutputMode.external("/assets")).files();

    assertThat(files).containsKeys("/fonts/inter.woff2", "/fonts/inter-italic.woff2");
    assertThat(files.get("/fonts/inter.woff2").content()).containsExactly(1, 2, 3);
  }

  @Test
  @DisplayName("every page preloads only the preloaded faces, before the stylesheet")
  void preloadsOnEveryPage() {
    for (var output : List.of(OutputMode.external("/assets"), OutputMode.inline())) {
      for (var html : site(output).pages().values()) {
        var head = Jsoup.parse(html).head();
        var preloads = head.select("link[rel=preload][as=font]");
        assertThat(preloads).hasSize(1);
        assertThat(preloads.getFirst().attr("href")).isEqualTo("/fonts/inter.woff2");
        assertThat(preloads.getFirst().attr("type")).isEqualTo("font/woff2");
        assertThat(preloads.getFirst().hasAttr("crossorigin")).isTrue();
        assertThat(html.indexOf("rel=\"preload\""))
            .isLessThan(
                html.indexOf(output instanceof OutputMode.Inline ? "<style>" : "stylesheet"));
      }
    }
  }

  @Test
  @DisplayName("a font file that shares a path with another file is reported")
  void reportsCollisions() {
    var site =
        SiteDefinition.of(SiteRoute.of("/", PageDocument.of("Home", main().text("Home"))))
            .withAssets(List.of(INTER))
            .withFonts(List.of(FontFace.of("Inter", INTER)));

    assertThatThrownBy(() -> Compiler.forSite().compile(site))
        .isInstanceOfSatisfying(
            CompilationException.class,
            failure ->
                assertThat(failure.diagnostics())
                    .extracting(diagnostic -> diagnostic.rule())
                    .contains("file-path-collision"));
  }

  @Test
  @DisplayName("a blank family is rejected")
  void rejectsBlankFamily() {
    assertThatThrownBy(() -> FontFace.of(" ", INTER)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("weights must lie within 1 to 1000, and a range must run low to high")
  void rejectsInvalidWeights() {
    var face = FontFace.of("Inter", Asset.of("/fonts/inter.woff2", new byte[] {1}));

    assertThatThrownBy(() -> face.weight(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> face.weight(100, 1001)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> face.weight(900, 100)).isInstanceOf(IllegalArgumentException.class);
    assertThat(face.weight(100, 900).weight()).isEqualTo("100 900");
  }
}
