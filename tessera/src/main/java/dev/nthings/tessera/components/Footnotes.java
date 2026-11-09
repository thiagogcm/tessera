package dev.nthings.tessera.components;

import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Endnotes list for the page's [Footnote] references, each note linking back to its reference.
/// Create one with [#of()] to place the list; without one, the list is appended at the end of the
/// document. The list is filled after layout, so it covers every reference on the page. Its
/// heading is always an `<h2>`, and link names such as "Back to reference 1" are fixed English.
public final class Footnotes extends BuiltinComponent {

  /// Numbered footnote registered on the page: the reference marker and the endnote it links to.
  record Entry(int number, ElementRef reference, ElementRef note, Element content) {}

  private static final class Registry {
    private final List<Entry> entries = new ArrayList<>();
    private boolean sectionPlaced;
  }

  private static final DocumentKey<Registry> REGISTRY =
      DocumentKey.of("footnotes", Registry.class, Registry::new);

  private String title = "Footnotes";

  private Footnotes() {}

  /// Creates an endnotes list titled "Footnotes". Use one per page: a second one repeats every
  /// note and reports `duplicate-id`.
  public static Footnotes of() {
    return new Footnotes();
  }

  /// Sets the visible heading, which also names the list. The default is `Footnotes`.
  public Footnotes title(String title) {
    this.title = title;
    return this;
  }

  /// Registers the next footnote on the page. `key` names its reference ids, or `null` to use its
  /// number.
  static Entry register(RenderContext context, @Nullable String key, Element content) {
    var registry = context.document(REGISTRY);
    var number = registry.entries.size() + 1;
    var name = key != null ? key : Integer.toString(number);
    var entry =
        new Entry(
            number, ElementRef.scoped("fnref-" + name), ElementRef.scoped("fn-" + number), content);
    registry.entries.add(entry);
    // Without an explicit list on the page, endnotes are appended after all content.
    context.atDocumentEnd(
        REGISTRY, _ -> registry.sectionPlaced ? Elements.empty() : new Footnotes());
    return entry;
  }

  @Override
  IntoElement render(RenderContext context) {
    var registry = context.document(REGISTRY);
    registry.sectionPlaced = true;
    var heading = title;
    return Elements.late(
        _ ->
            registry.entries.isEmpty()
                ? Elements.empty()
                : new EndnotesView(heading, List.copyOf(registry.entries)));
  }

  /// The endnotes list, a view for its scoped styles.
  private static final class EndnotesView extends BuiltinView {

    private final String title;
    private final List<Entry> entries;

    EndnotesView(String title, List<Entry> entries) {
      this.id = "footnotes";
      this.title = title;
      this.entries = entries;
    }

    @Override
    String viewType() {
      return "footnotes-section";
    }

    @Override
    void configureScope(ScopeStyle scope) {
      scope
          .root(
              s ->
                  s.mt(Tokens.SPACE_10)
                      .pt(Tokens.SPACE_5)
                      .borderBlockStart(Border.hairline(Tokens.BORDER_SUBTLE))
                      .fontSize(Tokens.FONT_SIZE_SM)
                      .textColor(Tokens.TEXT_MUTED))
          .select(":scope ol", s -> s.flex().flexCol().gap(Tokens.SPACE_2).ps(Tokens.SPACE_6))
          .select(
              ":scope li:target",
              s ->
                  s.highlight(Tone.ACCENT)
                      .px(Tokens.SPACE_2)
                      .py(Tokens.SPACE_1)
                      .textColor(Tokens.TEXT_PRIMARY));
    }

    @Override
    IntoElement render(RenderContext context) {
      var ol = Elements.ol().tabularNums();
      for (var entry : entries) {
        var backlink =
            Elements.a(entry.reference())
                .role(Role.DOC_BACKLINK)
                .ariaLabel("Back to reference " + entry.number())
                .textColor(Tokens.ACCENT_PRIMARY)
                .px(Tokens.SPACE_1)
                .text("↩");

        var li =
            Elements.li()
                .id(entry.note())
                .role(Role.DOC_FOOTNOTE)
                .children(entry.content(), Elements.text(" "), backlink);
        ol.child(li);
      }

      return Elements.aside()
          .role(Role.DOC_ENDNOTES)
          .ariaLabel(title)
          .children(
              Elements.h2()
                  .fontSize(Tokens.FONT_SIZE_MD)
                  .textColor(Tokens.TEXT_PRIMARY)
                  .py(Tokens.SPACE_2)
                  .text(title),
              ol);
    }
  }
}
