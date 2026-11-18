package dev.nthings.tessera.document.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import org.jspecify.annotations.Nullable;

/// One footnote definition: its body, an optional identity, and an optional preview. Place
/// [#reference()] at each occurrence. The first occurrence on the page assigns the note's number;
/// later occurrences reuse it and add their own backlink. The body is expanded once, in the
/// [Footnotes] list. A plain-text note previews that text, because the preview is new text and not
/// a second copy of an element. A markup body is not copied into the preview; supply
/// [#preview(String)] or [#preview(IntoElement)] when a hover preview is wanted. A preview element
/// is placed at every reference, so keep element ids out of a preview that is reused.
///
/// Set [#id(String)] before the note is referenced. It names the endnote in the footnotes scope
/// (`fn-<id>`). Without it, the number is used. Reusing an id for two definitions reports
/// `duplicate-id`. Link names such as "Footnote 1" are fixed English. This registry is a document
/// contribution, not a portable component.
public final class Footnote {

  private final Element body;
  private final @Nullable String plainText;
  private @Nullable String id;
  private @Nullable String previewText;
  private @Nullable Element preview;

  private Footnote(Element body, @Nullable String plainText) {
    this.body = body;
    this.plainText = plainText;
  }

  /// A note whose body is the plain text `text`. Each reference previews that text.
  public static Footnote of(String text) {
    return new Footnote(Elements.text(text), text);
  }

  /// A note whose body is `body`. The body is placed only in the endnote. There is no preview until
  /// [#preview(String)] or [#preview(IntoElement)].
  public static Footnote of(IntoElement body) {
    return new Footnote(body.intoElement(), null);
  }

  /// Names the endnote `fn-<id>` inside the footnotes scope. Characters outside `[A-Za-z0-9_-]`
  /// become `-`.
  public Footnote id(String id) {
    this.id = id;
    return this;
  }

  /// Shows `text` in each reference's hover preview instead of the default plain-text preview.
  public Footnote preview(String text) {
    this.previewText = text;
    this.preview = null;
    return this;
  }

  /// Shows `preview` in each reference's hover preview. The note body is not used as the preview.
  /// The same preview element is placed at every reference.
  public Footnote preview(IntoElement preview) {
    this.preview = preview.intoElement();
    this.previewText = null;
    return this;
  }

  /// A reference to this note. Each call is a separate occurrence with its own marker and backlink.
  /// Numbering and the endnote list are document assembly, so the reference is document content:
  /// place it in a [dev.nthings.tessera.document.DocumentElement], such as a paragraph.
  public DocumentContent reference() {
    return new FootnoteReference(this);
  }

  @Nullable String id() {
    return id;
  }

  Element body() {
    return body;
  }

  /// The preview for one reference, or `null` when this note has none. A plain-text preview is a
  /// new text node, not [#body()].
  @Nullable Element preview() {
    if (preview != null) {
      return preview;
    }
    if (previewText != null) {
      return Elements.text(previewText);
    }
    if (plainText != null) {
      return Elements.text(plainText);
    }
    return null;
  }

  /// One reference occurrence. The compiler assigns its marker an id in the enclosing scope.
  static final class FootnoteReference implements DocumentComponent {

    private final Footnote note;

    private FootnoteReference(Footnote note) {
      this.note = note;
    }

    @Override
    public DocumentContent expand(DocumentContext context) {
      var placed = Footnotes.reference(context, note);
      var entry = placed.entry();
      var label = "[" + entry.number() + "]";
      var link =
          Elements.a(entry.note())
              .role(Role.DOC_NOTEREF)
              .ariaLabel("Footnote " + entry.number())
              .textColor(Tokens.ACCENT_PRIMARY)
              .fontWeight(600)
              .text(label);
      var preview = note.preview();
      if (preview != null) {
        link.interestTooltip(tip ->
            tip.p(Tokens.SPACE_3)
                .maxInlineSize(CssLength.rem(22))
                .bg(Tokens.SURFACE_CARD)
                .border(Border.hairline(Tokens.BORDER_SUBTLE))
                .rounded(Tokens.RADIUS_LG)
                .child(preview));
      }
      return DocumentContent.of(
          Elements.sup().id(placed.marker()).scrollMarginBlockStart(CssLength.rem(7)).child(link));
    }
  }
}
