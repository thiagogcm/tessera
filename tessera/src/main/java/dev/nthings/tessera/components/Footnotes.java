package dev.nthings.tessera.components;

import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.HighlightTone;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Dual-mode Footnote system combining inline `interestfor` hover/focus preview popovers with
/// auto-numbered, bidirectionally linked WAI-ARIA Digital Publishing endnotes
/// (`role="doc-endnotes"`).
public final class Footnotes {

  /// Numbered footnote registered on the page; ids are fixed at the reference site.
  public record Entry(
      int number, String slug, String refDomId, String noteDomId, Element content) {}

  private static final class Registry {
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private boolean sectionPlaced;
  }

  private static final DocumentKey<Registry> REGISTRY =
      DocumentKey.of("footnotes", Registry.class, Registry::new);

  private Footnotes() {}

  private static Entry register(RenderContext ctx, String rawId, Element content) {
    var registry = ctx.document(REGISTRY);
    var local = ElementId.of(rawId);
    var slug = local.slug();
    var key = ctx.viewScope().extend(local).domId();
    var entry =
        new Entry(
            registry.entries.size() + 1,
            slug,
            ctx.viewScope().extend(ElementId.of("fnref-" + slug)).domId(),
            "fn-" + key,
            content);
    if (registry.entries.putIfAbsent(key, entry) != null) {
      throw new IllegalArgumentException("Duplicate footnote id '" + key + "' on the same page");
    }
    // Without an explicit section on the page, endnotes are appended after all content.
    ctx.atDocumentEnd(
        REGISTRY,
        _ -> registry.sectionPlaced ? Elements.empty() : new FootnotesSection("Footnotes"));
    return entry;
  }

  public static FootnoteRef ref(String id, String noteText) {
    return new FootnoteRef(id, Elements.text(noteText));
  }

  public static FootnoteRef ref(String id, IntoElement noteContent) {
    return new FootnoteRef(id, noteContent.intoElement());
  }

  public static FootnotesSection section() {
    return new FootnotesSection("Footnotes");
  }

  public static FootnotesSection section(String title) {
    return new FootnotesSection(title);
  }

  public record FootnoteRef(String id, Element content) implements RenderOnce {

    @Override
    public Element render(RenderContext context) {
      var entry = register(context, id, content);
      var label = "[" + entry.number() + "]";
      var triggerLink =
          Elements.a("#" + entry.noteDomId())
              .id("fnlink-" + entry.slug())
              .role(Role.DOC_NOTEREF)
              .ariaLabel("Footnote " + entry.number())
              .textColor(Tokens.ACCENT_PRIMARY)
              .fontWeight(600)
              .interestTooltip(
                  tip ->
                      tip.p(Tokens.SPACE_3)
                          .maxInlineSize(CssLength.rem(22))
                          .bg(Tokens.SURFACE_CARD)
                          .borderSubtle(Tokens.BORDER_SUBTLE)
                          .rounded(Tokens.RADIUS_LG)
                          .child(content))
              .text(label);

      return Elements.sup().id("fnref-" + entry.slug()).child(triggerLink);
    }
  }

  /// Endnotes list; rendered after layout so it lists every reference regardless of placement.
  public record FootnotesSection(String heading) implements RenderOnce {

    @Override
    public Element render(RenderContext context) {
      var registry = context.document(REGISTRY);
      registry.sectionPlaced = true;
      return Elements.late(
          _ ->
              registry.entries.isEmpty()
                  ? Elements.empty()
                  : new EndnotesView(heading, List.copyOf(registry.entries.values())));
    }
  }

  private record EndnotesView(String heading, List<Entry> entries) implements RenderView {

    @Override
    public String viewType() {
      return "footnotes-section";
    }

    @Override
    public ElementId entityId() {
      return ElementId.of("footnotes");
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(
              s ->
                  s.mt(Tokens.SPACE_10)
                      .pt(Tokens.SPACE_5)
                      .borderBlockStartSubtle(Tokens.BORDER_SUBTLE)
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
            Elements.a("#" + entry.refDomId())
                .role(Role.DOC_BACKLINK)
                .ariaLabel("Back to reference " + entry.number())
                .textColor(Tokens.ACCENT_PRIMARY)
                .px(Tokens.SPACE_1)
                .text("↩");

        var li =
            Elements.li()
                .attr("id", entry.noteDomId())
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
