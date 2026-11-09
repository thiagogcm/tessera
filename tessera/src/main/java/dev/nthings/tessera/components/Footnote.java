package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Tokens;
import org.jspecify.annotations.Nullable;

/// Numbered footnote reference. It shows its note as a hover or focus preview and links to the
/// note in the page's [Footnotes] list, which links back. References are numbered in the order
/// they render. Create one with [#of(String)] or [#of(IntoElement)]. Its link name, such as
/// "Footnote 1", is fixed English.
public final class Footnote extends BuiltinComponent {

  private final Element note;
  private @Nullable String id;

  private Footnote(Element note) {
    this.note = note;
  }

  /// Creates a reference whose note is the plain text `note`.
  public static Footnote of(String note) {
    return new Footnote(Elements.text(note));
  }

  /// Creates a reference whose note is `note`, which may contain markup. The note appears in both
  /// the preview and the endnotes, so keep element ids out of it.
  public static Footnote of(IntoElement note) {
    return new Footnote(note.intoElement());
  }

  /// Names the reference's DOM ids `fnref-<id>` and `fnlink-<id>` in the enclosing view scope;
  /// reusing an id there reports `duplicate-id`. Without it, the footnote number is used.
  public Footnote id(String id) {
    this.id = id;
    return this;
  }

  @Override
  IntoElement render(RenderContext context) {
    var key = id;
    var entry = Footnotes.register(context, key, note);
    var name = key != null ? key : Integer.toString(entry.number());
    var label = "[" + entry.number() + "]";
    var triggerLink =
        Elements.a(entry.note())
            .id(ElementRef.scoped("fnlink-" + name))
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
                        .child(note))
            .text(label);

    return Elements.sup().id(entry.reference()).child(triggerLink);
  }
}
