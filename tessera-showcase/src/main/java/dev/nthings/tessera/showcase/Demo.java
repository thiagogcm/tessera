package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.div;

import dev.nthings.tessera.components.CodeBlock;
import dev.nthings.tessera.components.ContentSection;
import dev.nthings.tessera.components.ContentSection.SurfaceKind;
import dev.nthings.tessera.components.Formatting;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;

/// One showcase section: what a component does, the platform features it compiles to, a live
/// example, and the Java that produced it.
final class Demo implements RenderOnce {

  private final String id;
  private final String title;
  private String summary = "";
  private List<String> features = List.of();
  private final List<Element> examples = new ArrayList<>();
  private List<String> code = List.of();
  private SurfaceKind surface = SurfaceKind.CARD;

  private Demo(String id, String title) {
    this.id = id;
    this.title = title;
  }

  static Demo of(String id, String title) {
    return new Demo(id, title);
  }

  Demo summary(String text) {
    this.summary = text;
    return this;
  }

  /// Platform features (CSS/HTML) the component relies on, shown as chips.
  Demo features(String... names) {
    this.features = List.of(names);
    return this;
  }

  Demo example(IntoElement... content) {
    for (var item : content) {
      examples.add(item.intoElement());
    }
    return this;
  }

  /// Java source shown under the example.
  Demo code(String... lines) {
    this.code = List.of(lines);
    return this;
  }

  Demo surface(SurfaceKind kind) {
    this.surface = kind;
    return this;
  }

  /// Inline row for controls that should keep their intrinsic width inside the stage.
  static TagElement row(IntoElement... controls) {
    return div().flex().flexWrap().itemsCenter().gap(CssLength.rem(0.75)).children(controls);
  }

  /// Card-surface styling shared by example panels, popovers and tooltips.
  static TagElement surface(TagElement box, CssLength radius) {
    return box.rounded(radius).bg(Tokens.SURFACE_CARD).borderSubtle(Tokens.BORDER_SUBTLE);
  }

  /// Labelled card-surface tile for layout and motion examples.
  static TagElement tile(String label) {
    return surface(div().p(CssLength.rem(1)).fontWeight(600).text(label), CssLength.rem(0.75));
  }

  /// Secondary button styled like the component triggers.
  static TagElement button(String label) {
    return surface(
            Elements.button().px(CssLength.rem(0.875)).py(CssLength.rem(0.5)).cursorPointer(),
            CssLength.rem(0.5))
        .text(label);
  }

  String id() {
    return id;
  }

  String title() {
    return title;
  }

  @Override
  public Element render() {
    var section = ContentSection.create(id, title).surface(surface).leadText(summary);
    if (!features.isEmpty()) {
      section.child(
          div()
              .attr("class", "demo-features")
              .ariaLabel("Platform features")
              .flex()
              .flexWrap()
              .gap(CssLength.rem(0.375))
              .children(features.stream().map(Formatting::inlineCode)));
    }
    section.child(
        div()
            .attr("class", "demo-stage")
            .flex()
            .flexCol()
            .gap(CssLength.rem(1))
            .p(CssLength.rem(1.25))
            .rounded(CssLength.rem(0.75))
            .bg(Tokens.SURFACE_CANVAS)
            .borderSubtle(Tokens.BORDER_SUBTLE)
            .children(examples));
    if (!code.isEmpty()) {
      section.child(CodeBlock.ofLines(code).filename("Usage").language("java"));
    }
    return section.intoElement();
  }
}
