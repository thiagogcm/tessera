package dev.nthings.tessera.document.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.core.ViewKey;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.Documents;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/// Endnotes list for the page's [Footnote] definitions. Each note appears once and links back to
/// every reference. Create one with [#of()] to place the list; without one, the list is appended at
/// the end of the document. The list is filled after layout, so it may precede the references and
/// still cover every one. Its heading is an `<h2>` and is not a document-outline heading. Link
/// names such as "Back to reference 1" are fixed English. A second list on the page repeats every
/// note and reports `duplicate-id`.
public final class Footnotes implements DocumentComponent {

  /// One definition on the page, numbered by its first reference, with every later occurrence.
  static final class Entry {
    private final int number;
    private final ElementRef note;
    private final Element body;
    private final List<ElementRef> markers = new ArrayList<>();

    private Entry(int number, String key, Element body) {
      this.number = number;
      this.note = ElementRef.scoped("fn-" + key);
      this.body = body;
    }

    int number() {
      return number;
    }

    ElementRef note() {
      return note;
    }

    Element body() {
      return body;
    }

    List<ElementRef> markers() {
      return markers;
    }
  }

  /// The occurrence just placed, with an identity independent of the note's author-supplied id.
  record Placed(Entry entry, ElementRef marker) {}

  private static final class Registry {
    private final Map<Footnote, Entry> byDefinition = new IdentityHashMap<>();
    private final List<Entry> entries = new ArrayList<>();
    private boolean sectionPlaced;
  }

  private static final ViewKey<Registry> REGISTRY =
      ViewKey.of("footnotes", Registry.class, Registry::new);

  private String title = "Footnotes";

  private Footnotes() {}

  /// Creates an endnotes list titled "Footnotes". Use one per page.
  public static Footnotes of() {
    return new Footnotes();
  }

  /// Sets the visible heading, which also names the list. The default is `Footnotes`.
  public Footnotes title(String title) {
    this.title = title;
    return this;
  }

  /// Records one reference to `note`. The first reference assigns the note's number and endnote id.
  /// Each reference gets its own marker. Without an explicit list, endnotes are appended after all
  /// content.
  static Placed reference(DocumentContext context, Footnote note) {
    var registry = context.state(REGISTRY);
    var entry = registry.byDefinition.get(note);
    if (entry == null) {
      var number = registry.entries.size() + 1;
      var id = note.id();
      var key = id != null ? id : Integer.toString(number);
      entry = new Entry(number, key, note.body());
      registry.byDefinition.put(note, entry);
      registry.entries.add(entry);
    }
    var marker = ElementRef.anonymous();
    entry.markers().add(marker);
    context.atDocumentEnd(
        REGISTRY,
        _ -> registry.sectionPlaced ? DocumentContent.of(Elements.empty()) : new Footnotes());
    return new Placed(entry, marker);
  }

  @Override
  public DocumentContent expand(DocumentContext context) {
    var registry = context.state(REGISTRY);
    registry.sectionPlaced = true;
    var heading = title;
    return Documents.late(_ ->
        DocumentContent.of(
            registry.entries.isEmpty()
                ? Elements.empty()
                : new EndnotesView(heading, List.copyOf(registry.entries))));
  }

  /// The endnotes list, a scope for its scoped styles.
  private static final class EndnotesView implements Scope {

    private final String title;
    private final List<Entry> entries;

    EndnotesView(String title, List<Entry> entries) {
      this.title = title;
      this.entries = entries;
    }

    @Override
    public String scopeId() {
      return "footnotes";
    }

    @Override
    public String scopeName() {
      return "footnotes-section";
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(s ->
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

    private static String backLabel(int number, int occurrence) {
      return occurrence == 1
          ? "Back to reference " + number
          : "Back to reference " + number + ", occurrence " + occurrence;
    }

    @Override
    public TagElement expand(ViewContext context) {
      var ol = Elements.ol().tabularNums();
      for (var entry : entries) {
        var li =
            Elements.li()
                .id(entry.note())
                .role(Role.DOC_FOOTNOTE)
                .scrollMarginBlockStart(CssLength.rem(7))
                .child(entry.body());
        var occurrence = 1;
        for (var marker : entry.markers()) {
          li.child(Elements.text(" "))
              .child(
                  Elements.a(marker)
                      .role(Role.DOC_BACKLINK)
                      .ariaLabel(backLabel(entry.number(), occurrence))
                      .textColor(Tokens.ACCENT_PRIMARY)
                      .px(Tokens.SPACE_1)
                      .text("↩"));
          occurrence++;
        }
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
