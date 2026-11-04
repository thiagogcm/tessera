package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.HighlightTone;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;

/// Dual-mode Footnote system combining inline `interestfor` hover/focus preview popovers with
/// auto-numbered, bidirectionally linked WAI-ARIA Digital Publishing endnotes (`role="doc-
/// endnotes"`).
public final class Footnotes {

  private Footnotes() {}

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
    public Element render() {
      var entry = RenderContext.current().registerFootnote(id, content);
      String label = "[" + entry.number() + "]";
      var triggerLink =
          Elements.a("#" + entry.noteDomId())
              .id("fnlink-" + entry.slug())
              .role("doc-noteref")
              .ariaLabel("Footnote " + entry.number())
              .textColor(Tokens.ACCENT_PRIMARY)
              .fontWeight(600)
              .interestTooltip(
                  tip ->
                      tip.p(CssLength.rem(0.75))
                          .maxInlineSize(CssLength.rem(22))
                          .bg(Tokens.SURFACE_CARD)
                          .borderSubtle(Tokens.BORDER_SUBTLE)
                          .rounded(CssLength.rem(0.5))
                          .child(content))
              .text(label);

      return Elements.sup().id("fnref-" + entry.slug()).child(triggerLink);
    }
  }

  public record FootnotesSection(String heading) implements RenderView {

    @Override
    public ElementId entityId() {
      return ElementId.of("footnotes");
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(
              s ->
                  s.mt(CssLength.rem(2.5))
                      .pt(CssLength.rem(1.25))
                      .borderBlockStartSubtle(Tokens.BORDER_SUBTLE)
                      .textSize(CssLength.rem(0.875))
                      .textColor(Tokens.TEXT_MUTED))
          .select(
              ":scope ol", s -> s.flex().flexCol().gap(CssLength.rem(0.5)).ps(CssLength.rem(1.5)))
          .select(
              ":scope li:target",
              s ->
                  s.highlight(HighlightTone.ACCENT)
                      .px(CssLength.rem(0.5))
                      .py(CssLength.rem(0.25))
                      .textColor(Tokens.TEXT_PRIMARY));
    }

    @Override
    public Element render() {
      var ctx = RenderContext.current();
      ctx.markFootnotesRendered();
      var entries = ctx.footnotes();
      if (entries.isEmpty()) {
        return Elements.empty();
      }

      var ol = Elements.ol().tabularNums();
      for (var entry : entries) {
        var backlink =
            Elements.a("#" + entry.refDomId())
                .role("doc-backlink")
                .ariaLabel("Back to reference " + entry.number())
                .textColor(Tokens.ACCENT_PRIMARY)
                .px(CssLength.rem(0.25))
                .text("↩");

        var li =
            Elements.li()
                .attr("id", entry.noteDomId())
                .role("doc-footnote")
                .children(entry.content(), Elements.text(" "), backlink);
        ol.child(li);
      }

      return Elements.aside()
          .role("doc-endnotes")
          .ariaLabel(heading)
          .children(
              Elements.h2()
                  .textSize(CssLength.rem(1.0))
                  .textColor(Tokens.TEXT_PRIMARY)
                  .py(CssLength.rem(0.5))
                  .text(heading),
              ol);
    }
  }
}
