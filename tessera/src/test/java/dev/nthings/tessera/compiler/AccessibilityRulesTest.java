package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.deferred;
import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.el;
import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.core.Elements.late;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatCode;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;
import static dev.nthings.tessera.support.HtmlAssertions.catchThrowableOfType;
import static dev.nthings.tessera.support.HtmlAssertions.tuple;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.PopoverMode;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.View;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Page authoring diagnostics")
class AccessibilityRulesTest {

  static Stream<Arguments> violations() {
    return Stream.of(
        argumentSet(
            "button without text content",
            new PageDocument("Invalid Button", div().child(button())),
            "must have text content"),
        argumentSet(
            "decorative image without role=presentation",
            new PageDocument("Invalid Img", div().child(img("/deco.svg", "").dimensions(1, 1))),
            "role=\"presentation\""),
        argumentSet(
            "image with neither dimensions nor aspect-ratio",
            new PageDocument(
                "CLS Violation",
                main().child(img("/banner.jpg", "Banner without width/height or aspect-ratio"))),
            "aspect-ratio"));
  }

  record Panel(String id, Element content) implements View {
    @Override
    public String viewId() {
      return id;
    }

    @Override
    public Element render(RenderContext context) {
      return div().child(content);
    }
  }

  @Test
  @DisplayName(
      "reports independent errors with their lexical component paths, including late and deferred content")
  void reportsIndependentErrorsWithLexicalComponentPaths() {
    var missing = ElementRef.scoped("confirmation");
    var document =
        new PageDocument(
            "Settings",
            new Panel(
                "settings",
                div()
                    .children(
                        new Panel("profile", img("/avatar.png", "Profile photo")),
                        new Panel(
                            "actions",
                            div()
                                .children(
                                    late(
                                        _ ->
                                            button()
                                                .id(ElementRef.scoped("save"))
                                                .command(missing, BuiltinCommand.SHOW_MODAL)
                                                .text("Save")),
                                    a("#details%ZZ")
                                        .id(ElementRef.scoped("details"))
                                        .text("Details"),
                                    deferred(button().id(ElementRef.scoped("cancel")), 10))))));

    var failure =
        catchThrowableOfType(CompilationException.class, () -> Compiler.compile(document));

    assertThat(failure.diagnostics())
        .extracting(
            Diagnostic::rule,
            d -> d.location().page(),
            d -> d.location().components(),
            d -> d.location().tag(),
            d -> d.location().domId())
        .containsExactlyInAnyOrder(
            tuple(
                "image-dimensions",
                "Settings",
                List.of("panel[settings]", "panel[settings--profile]"),
                "img",
                null),
            tuple(
                "unbound-reference",
                "Settings",
                List.of("panel[settings]", "panel[settings--actions]"),
                "button",
                "settings--actions--save"),
            tuple(
                "invalid-url",
                "Settings",
                List.of("panel[settings]", "panel[settings--actions]"),
                "a",
                "settings--actions--details"),
            tuple(
                "interactive-label",
                "Settings",
                List.of("panel[settings]", "panel[settings--actions]"),
                "button",
                "settings--actions--cancel"));
    assertThat(failure.getMessage())
        .contains("Settings", "settings--actions--save", "Fix:", ".id(ref)");
  }

  record DocumentExtras() implements View {
    private static final DocumentKey<Object> ACTIONS =
        DocumentKey.of("extra-actions", Object.class, Object::new);

    @Override
    public String viewId() {
      return "extras";
    }

    @Override
    public Element render(RenderContext context) {
      context.contributeHead(
          el(HtmlTag.LINK).attr("rel", "stylesheet").attr("href", "/styles%ZZ.css"));
      context.atDocumentEnd(ACTIONS, _ -> button().id(ElementRef.scoped("subscribe")));
      return div();
    }
  }

  @Test
  @DisplayName("head and end-of-document errors identify the component that registered the content")
  void reportsContributionOrigins() {
    var document =
        new PageDocument("Settings", new Panel("settings", new DocumentExtras().intoElement()));

    var failure =
        catchThrowableOfType(CompilationException.class, () -> Compiler.compile(document));

    assertThat(failure.diagnostics())
        .extracting(Diagnostic::rule, d -> d.location().components(), d -> d.location().tag())
        .containsExactlyInAnyOrder(
            tuple(
                "invalid-url",
                List.of("panel[settings]", "document-extras[settings--extras]"),
                "link"),
            tuple(
                "interactive-label",
                List.of("panel[settings]", "document-extras[settings--extras]"),
                "button"));
  }

  @Test
  @DisplayName(
      "an unresolved label reports its binding error without an additional missing-label error")
  void unresolvedLabelDoesNotProduceSecondaryLabelError() {
    var label = ElementRef.scoped("save-label");
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () -> Compiler.compile(new PageDocument("Editor", button().ariaLabelledBy(label))));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(d -> assertThat(d.rule()).isEqualTo("unbound-reference"));
  }

  @ParameterizedTest
  @EnumSource(BuiltinCommand.class)
  @DisplayName(
      "rejects native commands aimed at an ordinary element and accepts compatible targets")
  void checksNativeCommandTargets(BuiltinCommand command) {
    var target = div().id(ElementRef.scoped("target"));
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () ->
                Compiler.compile(
                    new PageDocument(
                        "Commands",
                        main()
                            .children(
                                button().command(target.ref(), command).text("Run"), target))));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.rule()).isEqualTo("incompatible-command-target");
              assertThat(d.message()).contains(command.wireValue(), "<div>", "target");
            });

    var compatible =
        switch (command) {
          case SHOW_MODAL, CLOSE, REQUEST_CLOSE ->
              dialog().id(ElementRef.scoped("target")).ariaLabel("Target");
          case SHOW_POPOVER, HIDE_POPOVER, TOGGLE_POPOVER ->
              div().id(ElementRef.scoped("target")).popover(PopoverMode.AUTO);
        };
    assertThatCode(
            () ->
                Compiler.compile(
                    new PageDocument(
                        "Commands",
                        main()
                            .children(
                                button().command(compatible.ref(), command).text("Run"),
                                deferred(compatible, 10)))))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @MethodSource("violations")
  @DisplayName("rejects invalid markup")
  void rejectsInvalidMarkup(PageDocument document, String message) {
    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(() -> Compiler.compile(document))
        .withMessageContaining(message);
  }

  @Test
  @DisplayName("accepts decorative images and links labelled by aria-label")
  void acceptsDecorativeImagesAndAriaLabelledLinks() {
    var document =
        new PageDocument(
            "Valid A11y",
            div()
                .children(
                    img("/deco.svg", "").role(Role.PRESENTATION).dimensions(100, 100),
                    a("/home")
                        .ariaLabel("Home page")
                        .child(new TagElement(HtmlTag.SPAN).ariaHidden(true))));

    assertThatCode(() -> Compiler.compile(document)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("percent encoded fragment links resolve without treating plus as space")
  void percentEncodedFragmentLinksResolveWithoutTreatingPlusAsSpace() {
    var document =
        new PageDocument(
            "Links",
            main()
                .id(ElementRef.page("café+intro"))
                .child(a("#caf%C3%A9%2Bintro").text("Introduction")));

    assertThatCode(() -> Compiler.compile(document)).doesNotThrowAnyException();
  }
}
