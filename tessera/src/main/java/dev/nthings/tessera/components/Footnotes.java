package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.HighlightTone;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;

/// Numbered footnotes. Each [#ref(String, String)] shows its note as a hover or focus preview
/// and links to an endnote, and each endnote links back to its reference. References are
/// numbered in the order they render. Endnotes go at the end of the document unless
/// [#section()] places them. The endnotes heading is always an `<h2>`, and link names such as
/// "Footnote 1" are fixed English text.
public final class Footnotes {

  /// Numbered footnote registered on the page: the reference marker and the endnote it links to.
  private record Entry(int number, ElementRef reference, ElementRef note, Element content) {}

  private static final class Registry {
    private final List<Entry> entries = new ArrayList<>();
    private boolean sectionPlaced;
  }

  private static final DocumentKey<Registry> REGISTRY =
      DocumentKey.of("footnotes", Registry.class, Registry::new);

  private Footnotes() {}

  private static Entry register(RenderContext ctx, ElementRef reference, Element content) {
    var registry = ctx.document(REGISTRY);
    var number = registry.entries.size() + 1;
    var entry = new Entry(number, reference, ElementRef.scoped("fn-" + number), content);
    registry.entries.add(entry);
    // Without an explicit section on the page, endnotes are appended after all content.
    ctx.atDocumentEnd(
        REGISTRY,
        _ -> registry.sectionPlaced ? Elements.empty() : new FootnotesSection("Footnotes"));
    return entry;
  }

  /// Creates a reference whose note is the plain text `noteText`. `id` names the footnote and
  /// must be unique within its view scope; a duplicate reports `duplicate-id`.
  public static FootnoteRef ref(String id, String noteText) {
    return new FootnoteRef(id, Elements.text(noteText));
  }

  /// Creates a reference whose note is `noteContent`, which may contain markup. The content
  /// appears in both the preview and the endnotes, so keep element ids out of it.
  public static FootnoteRef ref(String id, IntoElement noteContent) {
    return new FootnoteRef(id, noteContent.intoElement());
  }

  /// Places the endnotes list here, titled "Footnotes". Without a section, the list is appended
  /// after the document content. Use one section per page: a second one repeats every note and
  /// reports `duplicate-id`.
  public static FootnotesSection section() {
    return new FootnotesSection("Footnotes");
  }

  /// Places the endnotes list here, titled `title`. The title is the list's heading and its
  /// accessible name.
  public static FootnotesSection section(String title) {
    return new FootnotesSection(title);
  }

  /// Inline footnote marker. Create it with [Footnotes#ref(String, String)].
  public record FootnoteRef(String id, Element content) implements Component {

    @Override
    public Element render(RenderContext context) {
      var entry = register(context, ElementRef.scoped("fnref-" + id), content);
      var label = "[" + entry.number() + "]";
      var triggerLink =
          Elements.a(entry.note())
              .id(ElementRef.scoped("fnlink-" + id))
              .role(Role.DOC_NOTEREF)
              .ariaLabel("Footnote " + entry.number())
              .textColor(Tokens.ACCENT_PRIMARY)
              .fontWeight(600)
              .interestTooltip(
                  tip ->
                      tip.p(Tokens.SPACE_3)
                          .maxInlineSize(CssLength.rem(22))
                          .bg(Tokens.SURFACE_CARD)
                          .border(Border.hairline(Tokens.BORDER_SUBTLE))
                          .rounded(Tokens.RADIUS_LG)
                          .child(content))
              .text(label);

      return Elements.sup().id(entry.reference()).child(triggerLink);
    }
  }

  /// Endnotes list at this position. The list is filled after layout, so it covers every
  /// reference on the page.
  public record FootnotesSection(String heading) implements Component {

    @Override
    public Element render(RenderContext context) {
      var registry = context.document(REGISTRY);
      registry.sectionPlaced = true;
      return Elements.late(
          _ ->
              registry.entries.isEmpty()
                  ? Elements.empty()
                  : new EndnotesView(heading, List.copyOf(registry.entries)));
    }
  }

  private record EndnotesView(String heading, List<Entry> entries) implements View {

    @Override
    public String viewType() {
      return "footnotes-section";
    }

    @Override
    public String viewId() {
      return "footnotes";
    }

    @Override
    public void configureScope(ScopeStyle scope) {
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
                  s.highlight(HighlightTone.ACCENT)
                      .px(Tokens.SPACE_2)
                      .py(Tokens.SPACE_1)
                      .textColor(Tokens.TEXT_PRIMARY));
    }

    @Override
    public Element render(RenderContext context) {
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
          .ariaLabel(heading)
          .children(
              Elements.h2()
                  .fontSize(Tokens.FONT_SIZE_MD)
                  .textColor(Tokens.TEXT_PRIMARY)
                  .py(Tokens.SPACE_2)
                  .text(heading),
              ol);
    }
  }
}
