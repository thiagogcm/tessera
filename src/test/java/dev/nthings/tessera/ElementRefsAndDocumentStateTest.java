package dev.nthings.tessera;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.nthings.tessera.components.Callout;
import dev.nthings.tessera.components.Footnotes;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.components.Toc;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.render.AccessibilityViolationException;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ElementRefsAndDocumentStateTest {

  /// View wrapping arbitrary content so tests can place elements in a nested scope.
  record Panel(String id, Element content) implements RenderView {
    @Override
    public ElementId entityId() {
      return ElementId.of(id);
    }

    @Override
    public Element render() {
      return div().child(content);
    }
  }

  private static Document render(Element body) {
    return Jsoup.parse(PageCompiler.compile(new PageDocument("Test", body)).html());
  }

  @Test
  @DisplayName("Ref resolves to its bind-site scope from an invoker in another view")
  void refResolvesAcrossViews() {
    var target = ElementRef.named("confirm");
    var parsed =
        render(
            main()
                .children(
                    new Panel(
                        "toolbar",
                        button().command(target, BuiltinCommand.SHOW_MODAL).text("Open")),
                    new Panel("dialogs", dialog().id(target).text("Sure?"))));

    assertThat(parsed.selectFirst("button").attr("commandfor")).isEqualTo("dialogs--confirm");
    assertThat(parsed.getElementById("dialogs--confirm")).isNotNull();
  }

  @Test
  @DisplayName("Unbound ref and duplicate DOM ids fail compilation")
  void rejectsUnboundRefsAndDuplicateIds() {
    var dangling = ElementRef.named("ghost");
    assertThatThrownBy(
            () -> render(div().child(button().command(dangling, BuiltinCommand.CLOSE).text("x"))))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("ElementRef(ghost)")
        .hasMessageContaining("not bound");

    assertThatThrownBy(() -> render(div().children(p().id("dup"), p().id("dup"))))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("Duplicate DOM id 'dup'");

    var shared = ElementRef.named("once");
    assertThatThrownBy(() -> render(div().children(p().id(shared), p().id(shared))))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("bound to more than one element");
  }

  @Test
  @DisplayName("Same-named form fields get distinct compiler-assigned scopes")
  void sameNamedFieldsDoNotCollide() {
    var parsed =
        render(
            main()
                .children(
                    Forms.field("Email", Forms.email("email")),
                    Forms.field("Newsletter email", Forms.email("email"))));

    var labels = parsed.select("label.field-label");
    assertThat(labels).hasSize(2);
    assertThat(labels.get(0).attr("for")).isNotEqualTo(labels.get(1).attr("for"));
    for (var label : labels) {
      assertThat(parsed.getElementById(label.attr("for"))).isNotNull();
    }
  }

  @Test
  @DisplayName("Context TOC placed before its sections lists them")
  void tocBeforeSections() {
    var parsed =
        render(
            main()
                .children(
                    Toc.fromContext("toc", "On this page"),
                    Toc.section("intro", "Introduction", p().text("a")),
                    Toc.section("usage", "Usage", p().text("b"))));

    var links = parsed.select("nav.toc-container a");
    assertThat(links).extracting(a -> a.attr("href")).containsExactly("#intro", "#usage");
  }

  @Test
  @DisplayName("Explicit footnotes section placed before references lists them once")
  void footnotesSectionBeforeReferences() {
    var parsed =
        render(
            main()
                .children(
                    Footnotes.section("Notes"),
                    p().child(Footnotes.ref("a", "First note")),
                    p().child(Footnotes.ref("b", "Second note"))));

    assertThat(parsed.select("aside[role=doc-endnotes]")).hasSize(1);
    assertThat(parsed.select("aside[role=doc-endnotes] li")).hasSize(2);
    assertThat(parsed.selectFirst("main > :first-child").attr("role")).isEqualTo("doc-endnotes");
  }

  @Test
  @DisplayName("Reusing a footnote or section id on one page fails with a clear error")
  void rejectsDuplicateFootnoteAndSectionIds() {
    assertThatThrownBy(
            () -> render(main().children(Footnotes.ref("n", "One"), Footnotes.ref("n", "Two"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Duplicate footnote id");
    assertThatThrownBy(
            () ->
                render(
                    main()
                        .children(
                            Toc.section("intro", "Intro", p()),
                            Toc.section("intro", "Again", p()))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Duplicate section id");
  }

  @Test
  @DisplayName("Builds are reproducible and compiling never mutates the input tree")
  void deterministicAutoIds() {
    var button = button().attr("class", "custom").px(CssLength.rem(1)).text("Go");
    var body = main().children(Callout.note("one"), Callout.tip("two"), button);
    var first = PageCompiler.compile(new PageDocument("T", body)).html();
    var second = PageCompiler.compile(new PageDocument("T", body)).html();
    var rebuilt =
        PageCompiler.compile(
                new PageDocument(
                    "T",
                    main()
                        .children(
                            Callout.note("one"),
                            Callout.tip("two"),
                            button().attr("class", "custom").px(CssLength.rem(1)).text("Go"))))
            .html();

    assertThat(second).isEqualTo(first);
    assertThat(rebuilt).isEqualTo(first);
    assertThat(button.attributes().get("class")).isEqualTo("custom");
  }
}
