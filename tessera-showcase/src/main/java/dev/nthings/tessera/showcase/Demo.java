package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.div;

import dev.nthings.tessera.components.CodeBlock;
import dev.nthings.tessera.components.Formatting;
import dev.nthings.tessera.components.Section;
import dev.nthings.tessera.core.DocumentContent;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.components.DocumentSection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// One showcase section: description, platform features, live example and its Java source.
final class Demo implements DocumentComponent {

  private final String id;
  private final String title;
  private String summary = "";
  private List<String> features = List.of();
  private final List<DocumentContent> examples = new ArrayList<>();
  private @Nullable Supplier<List<DocumentContent>> stage;
  private List<String> code = List.of();
  private Section.Surface surface = Section.Surface.CARD;

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

  Demo features(String... names) {
    this.features = List.of(names);
    return this;
  }

  /// Adds examples, portable or document content such as deferred dialogs or footnotes.
  Demo example(DocumentContent... content) {
    examples.addAll(List.of(content));
    return this;
  }

  /// Called on each render, so every compilation gets fresh elements.
  Demo stage(Supplier<List<DocumentContent>> content) {
    this.stage = content;
    return this;
  }

  Demo code(String... lines) {
    this.code = List.of(lines);
    return this;
  }

  Demo surface(Section.Surface kind) {
    this.surface = kind;
    return this;
  }

  /// Card styling shared by panels, popovers and tooltips.
  static TagElement surface(TagElement box, CssLength radius) {
    return box.rounded(radius)
        .bg(Tokens.SURFACE_CARD)
        .border(Border.hairline(Tokens.BORDER_SUBTLE));
  }

  static TagElement tile(String label) {
    return surface(div().p(CssLength.rem(1)).fontWeight(600).text(label), CssLength.rem(0.75));
  }

  String id() {
    return id;
  }

  String title() {
    return title;
  }

  @Override
  public DocumentContent expand(DocumentContext context) {
    var stageItems = new ArrayList<DocumentContent>(examples);
    var fresh = stage;
    if (fresh != null) {
      stageItems.addAll(fresh.get());
    }
    var section = Section.of(id, title).surface(surface).description(summary);
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
    var stageElement =
        DocumentElement.of(
                div()
                    .attr("class", "demo-stage")
                    .flex()
                    .flexCol()
                    .gap(CssLength.rem(1))
                    .p(CssLength.rem(1.25))
                    .rounded(CssLength.rem(0.75))
                    .bg(Tokens.SURFACE_CANVAS)
                    .border(Border.hairline(Tokens.BORDER_SUBTLE)))
            .children(stageItems);
    if (code.isEmpty()) {
      return DocumentSection.of(section, stageElement);
    }
    return DocumentSection.of(
        section,
        stageElement,
        CodeBlock.ofLines(code.toArray(String[]::new)).filename("Usage").language("java"));
  }
}
