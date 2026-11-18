package dev.nthings.tessera.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.errorprone.annotations.CheckReturnValue;
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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.owasp.encoder.Encode;

/// Compiles representative consumer snippets against the library, as an application would, to show
/// which placements the content families allow.
@DisplayName("Placement compilation")
class PlacementCompilationTest {

  private static final String IMPORTS =
      """
      import static dev.nthings.tessera.core.Elements.*;
      import dev.nthings.tessera.components.*;
      import dev.nthings.tessera.core.*;
      import dev.nthings.tessera.document.*;
      import dev.nthings.tessera.document.components.*;
      import dev.nthings.tessera.css.CssLength;
      import dev.nthings.tessera.compiler.Compiler;
      import dev.nthings.tessera.compiler.PageDocument;
      import java.util.List;
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
        "IntoElement x = div().child(Documents.rawHtml(\"<b>x</b>\"));",
        "IntoElement x = p().children(Footnote.of(\"x\").reference());",
        "IntoElement x = Callout.of(Callout.Kind.NOTE).child(Footnote.of(\"x\").reference());",
        "IntoElement x = Card.of(\"Title\").body(Documents.late(c -> Documents.rawHtml(\"\")));",
        "IntoElement x = Prose.of(Image.of(\"/a.png\", \"A\").hero());",
        "Component c = context -> Documents.rawHtml(\"x\");",
        "IntoElement x = div().child(TableOfContents.of(\"Contents\"));",
        "IntoElement x = div().children(List.<DocumentContent>of(Documents.rawHtml(\"x\")));",
        "IntoElement x = Callout.of(Callout.Kind.NOTE).children(List.of(Footnote.of(\"x\").reference()));",
      })
  @DisplayName("document content cannot be placed in portable content")
  void documentContentCannotBePlacedInPortableContent(String statement) {
    assertThat(compile(statement)).isNotEmpty().allMatch(TYPE_ERRORS::contains);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "IntoElement x = el(HtmlTag.SCRIPT);",
      })
  @DisplayName("portable content has no script element")
  void portableContentHasNoScriptElement(String statement) {
    assertThat(compile(statement)).containsExactly("compiler.err.cant.resolve.location");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "DocumentContent x = DocumentElement.of(p()).child(Footnote.of(\"x\").reference());",
        "DocumentContent x = DocLayout.of(Section.of(\"a\", \"A\"), Section.of(\"b\", \"B\"));",
        "IntoElement x = Callout.of(Callout.Kind.NOTE).child(p().text(\"Portable\"));",
      })
  @DisplayName("portable content can be placed in document content")
  void portableContentCanBePlacedInDocumentContent(String statement) {
    assertThat(compile(statement)).isEmpty();
  }

  @Test
  @DisplayName("a static consumer uses document and CSS authoring without the browser module")
  void staticConsumerUsesDocumentAndCssAuthoring() {
    assertThat(
            compile(
                """
                Scope scope = _ -> div().p(CssLength.px(6));
                var body = DocumentElement.of(main()).child(scope).child(Footnote.of("Note").reference());
                Compiler.compile(
                    new PageDocument("Consumer", body));
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
  @DisplayName("the authoring module hides its linked trees and collection")
  void staticConsumerCannotImportInternals(String importedType) {
    assertThat(compile(importedType, "")).containsExactly("compiler.err.package.not.visible");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "div().attributes();",
        "div().style();",
        "fragment().childrenList();",
        "Documents.rawHtml(\"\").documentNode();"
      })
  @DisplayName("builders expose no inspection getters or lowered nodes")
  void buildersExposeNoInspectionGetters(String statement) {
    assertThat(compile(statement)).containsExactly("compiler.err.cant.resolve.location.args");
  }

  @Test
  @DisplayName("tag constructors are private to their package")
  void tagConstructorIsHidden() {
    assertThat(compile("new TagElement(HtmlTag.DIV);"))
        .containsExactly("compiler.err.cant.apply.symbol");
  }

  private List<String> compile(String statement) {
    return compile("", statement);
  }

  private List<String> compile(String extraImport, String statement) {
    var sources =
        List.of(
            source("module-info", "module consumer { requires dev.nthings.tessera; }"),
            source(
                "consumer/Snippet",
                "package consumer;\n"
                    + IMPORTS
                    + extraImport
                    + "\nclass Snippet { void run() { "
                    + statement
                    + " } }"));
    var modulePath =
        List.of(Element.class, Nullable.class, CheckReturnValue.class, Encode.class).stream()
            .map(PlacementCompilationTest::location)
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
}
