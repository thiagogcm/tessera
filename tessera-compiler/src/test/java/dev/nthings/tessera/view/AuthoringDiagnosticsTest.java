package dev.nthings.tessera.view;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.fragment;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.support.Snapshots.render;
import static dev.nthings.tessera.view.LiveDom.page;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.SvgTag;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.Documents;
import dev.nthings.tessera.support.Diagnostics;
import dev.nthings.tessera.support.Diagnostics.Found;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Authoring diagnostics in both backends")
class AuthoringDiagnosticsTest {

  /// The rules and tags each backend reports for `content`.
  private static void assertBothReport(IntoElement content, String rule, String tag) {
    assertThat(Diagnostics.of(() -> page(content)))
        .extracting(Found::rule, Found::tag)
        .containsExactly(tuple(rule, tag));
    assertThat(Diagnostics.of(() -> render(content)))
        .extracting(Found::rule, Found::tag)
        .containsExactly(tuple(rule, tag));
  }

  static Stream<Arguments> unnamedControls() {
    return Stream.of(
        argumentSet(
            "close button showing only an aria-hidden glyph",
            button().child(span().ariaHidden(true).text("×")),
            "button"),
        argumentSet(
            "link whose only text is hidden",
            a("/next").child(span().boolAttr("hidden").text("Next")),
            "a"),
        argumentSet(
            "button whose text sits under an aria-hidden wrapper",
            button().child(span().ariaHidden(true).child(span().text("Save"))),
            "button"));
  }

  @ParameterizedTest
  @MethodSource("unnamedControls")
  @DisplayName("a control whose only text is hidden from assistive technology has no name")
  void hiddenTextDoesNotName(TagElement control, String tag) {
    assertBothReport(main().child(control), "interactive-label", tag);
  }

  @Test
  @DisplayName("hidden decoration beside visible text or an aria-label still names a control")
  void visibleTextOrLabelNames() {
    var content =
        main()
            .children(
                button().ariaLabel("Close").child(span().ariaHidden(true).text("×")),
                button().children(span().ariaHidden(true).text("+"), span().text("Add")),
                a("/next").children(span().text("Next"), span().ariaHidden(true).text("→")));

    page(content);
    render(content);
  }

  static Stream<Arguments> invalidControlDefaults() {
    return Stream.of(
        argumentSet(
            "value on a file input",
            Elements.input("FILE").initialValue("x"),
            "initial-value",
            "input"),
        argumentSet(
            "checked on a text input",
            Elements.input("text").initialChecked(false),
            "initial-checked",
            "input"),
        argumentSet(
            "single value on a multiple select",
            Elements.select().boolAttr("multiple").initialValue("x"),
            "initial-value",
            "select"),
        argumentSet(
            "selection on a single select",
            Elements.select().initialSelectedValues(List.of("x")),
            "initial-selection",
            "select"),
        argumentSet(
            "value attribute and initial value",
            Elements.input("text").attr("VALUE", "x").initialValue("x"),
            "initial-control-conflict",
            "input"),
        argumentSet(
            "checked attribute and initial checked",
            Elements.input("checkbox").initialChecked(false).boolAttr("CHECKED"),
            "initial-control-conflict",
            "input"),
        argumentSet(
            "textarea text and initial value",
            Elements.textarea().child(fragment().text("Old")).initialValue("New"),
            "initial-control-conflict",
            "textarea"),
        argumentSet(
            "selected option and initial value",
            Elements.select()
                .initialValue("x")
                .child(
                    fragment()
                        .child(
                            Elements.optgroup("Group")
                                .child(Elements.option("x", "X").boolAttr("selected").key("x")))),
            "initial-control-conflict",
            "option"));
  }

  @ParameterizedTest
  @MethodSource("invalidControlDefaults")
  @DisplayName("invalid or conflicting control defaults are reported at the authored control")
  void validatesControlDefaults(TagElement control, String rule, String tag) {
    assertBothReport(main().child(control), rule, tag);
  }

  @Test
  @DisplayName("a default matches options produced by late callbacks")
  void matchesLateOptions() {
    var select =
        DocumentElement.of(Elements.select().initialValue("Blue sky"))
            .child(
                Documents.late(_ ->
                    DocumentContent.of(
                        fragment()
                            .children(
                                Elements.option("red", "Red"),
                                Elements.optgroup("Group")
                                    .child(Elements.option().text("  Blue\t sky  ")),
                                Elements.option("Blue sky", "Duplicate")))));

    assertThat(
            page(DocumentElement.of(main()).child(select))
                .select("select option[selected]")
                .eachText())
        .containsExactly("Blue sky");
  }

  @Test
  @DisplayName("SVG shapes outside an svg root, and HTML inside one, are rejected")
  void svgNamespaceTransitionsAreValidated() {
    assertBothReport(
        main().child(div().child(Elements.svg(SvgTag.CIRCLE))), "svg-namespace", "circle");
    assertBothReport(main().child(Elements.svgIcon(16, 16).child(div())), "svg-namespace", "div");

    var valid = main().child(Elements.svgIcon(16, 16).child(Elements.svg(SvgTag.CIRCLE)));
    page(valid);
    render(valid);
  }

  @Test
  @DisplayName("deferred content is checked against the body it moves to, not its SVG parent")
  void deferredContentIsCheckedWhereItMoves() {
    var deferredShape =
        DocumentElement.of(Elements.svgIcon(16, 16))
            .child(Documents.deferred(Elements.svg(SvgTag.PATH).attr("d", "M0 0")));
    var deferredHtml =
        DocumentElement.of(Elements.svgIcon(16, 16))
            .child(Documents.deferred(p().text("Moved to the body")));

    assertThat(Diagnostics.rules(() -> page(deferredShape))).containsExactly("svg-namespace");
    assertThat(page(deferredHtml).select("body > p").text()).isEqualTo("Moved to the body");
  }

  @Test
  @DisplayName("script URLs are rejected whatever their case, spacing or attribute")
  void scriptUrlsAreRejected() {
    for (var url :
        List.of("javascript:alert(1)", "  JavaScript:alert(1)", "java\tscript:void(0)")) {
      var content =
          main().children(a(url).text("Run"), Elements.el(HtmlTag.A).attr("HREF", url).text("Run"));
      assertThat(Diagnostics.rules(() -> page(content)))
          .filteredOn("script-url"::equals)
          .hasSize(2);
      assertThat(Diagnostics.rules(() -> render(content))).contains("script-url");
    }
    var frame =
        main()
            .child(
                Elements.iframe()
                    .attr("title", "Inline")
                    .attr("SRC", "data:text/html,%3Cscript%3Ealert(1)%3C/script%3E"));
    assertThat(Diagnostics.rules(() -> page(frame))).contains("script-url");
    assertThat(Diagnostics.rules(() -> render(frame))).contains("script-url");
    page(main().child(a("/javascript:guide").text("Guide")));
  }
}
