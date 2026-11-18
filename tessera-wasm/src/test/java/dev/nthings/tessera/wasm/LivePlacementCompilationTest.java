package dev.nthings.tessera.wasm;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.core.Element;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/// Compiles representative consumer snippets against the library and the browser module, as an
/// application would, to show where live content can be placed.
@DisplayName("Live placement compilation")
class LivePlacementCompilationTest {

  private static final String IMPORTS =
      """
      import static dev.nthings.tessera.core.Elements.*;
      import dev.nthings.tessera.components.*;
      import dev.nthings.tessera.core.*;
      import dev.nthings.tessera.document.*;
      import dev.nthings.tessera.document.components.*;
      import java.util.List;
      import dev.nthings.tessera.wasm.*;
      """;

  /// Error codes of a type mismatch, as opposed to a missing symbol or a syntax error.
  private static final Set<String> TYPE_ERRORS =
      Set.of(
          "compiler.err.prob.found.req",
          "compiler.err.cant.apply.symbol",
          "compiler.err.cant.apply.symbols");

  @TempDir Path output;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "IntoElement x = div().child(LiveElement.of(button()).onClick(() -> {}));",
        "IntoElement x = Card.of(\"Title\").body(Live.effect(\"tick\", lifetime -> {}));",
        "Component c = context -> LiveElement.of(button());",
        "DocumentContent x = DocumentElement.of(div()).child(LiveElement.of(button()));",
        "DocumentContent x = DocumentContent.of(LiveElement.of(button()));",
        "LiveContent x = LiveElement.of(div()).child(Documents.rawHtml(\"<b>x</b>\"));",
        "LiveComponent c = context -> Footnote.of(\"x\").reference();",
        "LiveContent x = LiveContent.of(Documents.deferred(div()));",
        "IntoElement x = div().children(List.<LiveContent>of(LiveElement.of(button())));",
        "DocumentContent x = DocumentElement.of(div()).children(List.of(LiveElement.of(button())));",
        "LiveContent x = LiveElement.of(div()).children(List.<DocumentContent>of(Documents.rawHtml(\"x\")));",
      })
  @DisplayName("live content stays out of portable and document content, and the reverse")
  void liveContentStaysOutOfPortableAndDocumentContentAndTheReverse(String statement) {
    assertThat(compile(statement)).isNotEmpty().allMatch(TYPE_ERRORS::contains);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "LiveContent x = LiveElement.of(div()).child(Callout.of(Callout.Kind.NOTE))"
            + ".child(Live.effect(\"tick\", lifetime -> {}));",
        "LiveComponent c = context -> LiveElement.of(Button.of(\"Save\").element())"
            + ".onClick(context.root().update(() -> {}));",
        "LiveContent x = LiveContent.of(Section.of(\"a\", \"A\").child(p().text(\"Portable\")));",
      })
  @DisplayName("portable content can be placed in live content")
  void portableContentCanBePlacedInLiveContent(String statement) {
    assertThat(compile(statement)).isEmpty();
  }

  @Test
  @DisplayName("the browser consumer only needs the public live module")
  void consumerUsesTheLiveModule() {
    assertThat(
            compile(
                """
                LiveComponent app = context ->
                    LiveElement.of(button().text("Go")).on("click", event -> {});
                LiveScope shell = _ -> LiveElement.of(main());
                """))
        .isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "import dev.nthings.tessera.view.ViewSnapshot;",
        "import dev.nthings.tessera.internal.authoring.TagData;",
        "import dev.nthings.tessera.internal.css.StyleSheet;"
      })
  @DisplayName("requiring the browser module keeps Tessera internals encapsulated")
  void liveConsumerCannotImportInternals(String importedType) {
    assertThat(compile(importedType, "")).containsExactly("compiler.err.package.not.visible");
  }

  @Test
  @DisplayName("live elements expose no lowered view data")
  void liveElementsExposeNoLoweredViewData() {
    assertThat(compile("LiveElement.of(div()).liveNode();"))
        .containsExactly("compiler.err.cant.resolve.location.args");
  }

  private List<String> compile(String statement) {
    return compile("", statement);
  }

  private List<String> compile(String extraImport, String statement) {
    var sources =
        List.of(
            source("module-info", "module consumer { requires dev.nthings.tessera.wasm; }"),
            source(
                "consumer/Snippet",
                "package consumer;\n"
                    + IMPORTS
                    + extraImport
                    + "\nclass Snippet { void run() { "
                    + statement
                    + " } }"));
    var modulePath =
        List.of(
                Element.class,
                LiveElement.class,
                Nullable.class,
                JSObject.class,
                CheckReturnValue.class,
                encoder())
            .stream()
            .map(LivePlacementCompilationTest::location)
            .toList();
    var diagnostics = new DiagnosticCollector<JavaFileObject>();
    var task =
        ToolProvider.getSystemJavaCompiler()
            .getTask(
                null,
                null,
                diagnostics,
                List.of(
                    "-proc:none",
                    "--module-path",
                    String.join(File.pathSeparator, modulePath),
                    "-d",
                    output.toString()),
                null,
                sources);
    task.call();
    return diagnostics.getDiagnostics().stream()
        .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
        .map(Diagnostic::getCode)
        .toList();
  }

  private static SimpleJavaFileObject source(String name, String source) {
    return new SimpleJavaFileObject(
        URI.create("string:///" + name + ".java"), JavaFileObject.Kind.SOURCE) {
      @Override
      public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return source;
      }
    };
  }

  private static String location(Class<?> type) {
    try {
      return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  // The encoder is required to resolve Tessera on the module path. It is private to Tessera.
  private static Class<?> encoder() {
    try {
      return Class.forName("org.owasp.encoder.Encode", false, Element.class.getClassLoader());
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }
}
