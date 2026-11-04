package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.div;

import dev.nthings.tessera.components.CodeBlock;
import dev.nthings.tessera.components.ContentSection;
import dev.nthings.tessera.components.ContentSection.SurfaceKind;
import dev.nthings.tessera.components.Formatting;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// One showcase section: what a component does, the platform features it compiles to, a live
/// example, and the Java that produced it.
final class Demo implements RenderOnce {

  private final String id;
  private final String title;
  private String summary = "";
  private List<String> features = List.of();
  private final List<Element> examples = new ArrayList<>();
  private @Nullable List<String> code;
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
    List<String> source = code;
    if (source != null) {
      section.child(CodeBlock.ofLines(source).filename("Usage").language("java"));
    }
    return section.intoElement();
  }
}
