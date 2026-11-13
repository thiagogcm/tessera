package dev.nthings.tessera.components;

import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.DocumentHeading;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.ParentElement;
import dev.nthings.tessera.core.TagElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/// Native HTML heading, `h1` through `h6`, with phrasing content and an optional document-outline
/// entry. Create one with [#of(int, String)] when the visible text and the navigation label are
/// the same plain string, or [#of(int, String, IntoElement...)] when the visible title is rich
/// and the table of contents needs a separate plain label. Levels outside 1 through 6 throw
/// IllegalArgumentException. This primitive does not represent heading levels above 6.
///
/// The heading does not register itself. Call [#outline(ElementRef)] to put `label` in the
/// document outline, aimed at `target`. Dialog titles, footnote chrome, and other interface
/// headings leave the outline unset. [#id(ElementRef)] binds an anchor on the heading element
/// itself; a section can aim the outline at the section instead.
public final class Heading extends BuiltinComponent implements ParentElement<Heading> {

  private final int level;
  private final String label;
  private final List<Element> children = new ArrayList<>();
  private @Nullable ElementRef anchor;
  private @Nullable ElementRef outlineTarget;
  private @Nullable UnaryOperator<TagElement> presentation;

  private Heading(int level, String label) {
    this.level = HtmlTag.heading(level).headingLevel();
    this.label = label;
  }

  /// A level-`level` heading whose visible text and navigation label are both `text`.
  public static Heading of(int level, String text) {
    return new Heading(level, text).text(text);
  }

  /// A level-`level` heading. `label` is the plain navigation label. `content` is the visible
  /// phrasing and is not copied into the outline.
  public static Heading of(int level, String label, IntoElement... content) {
    return new Heading(level, label).children(content);
  }

  /// Binds `anchor` on the heading element.
  public Heading id(ElementRef anchor) {
    this.anchor = anchor;
    return this;
  }

  /// Includes this heading in the document outline, linking to the element bound to `target`.
  /// The outline uses [#of]'s plain label and this heading's level. Bind `target` on the page.
  public Heading outline(ElementRef target) {
    this.outlineTarget = target;
    return this;
  }

  /// Applies presentation to the heading element. `presentation` receives the heading and returns
  /// it. Section typography uses this; the heading itself has no surface styles.
  Heading present(UnaryOperator<TagElement> presentation) {
    this.presentation = presentation;
    return this;
  }

  @Override
  public Heading self() {
    return this;
  }

  @Override
  public void appendChild(Element child) {
    children.add(child);
  }

  @Override
  IntoElement expand(DocumentContext context) {
    var tag = Elements.el(HtmlTag.heading(level));
    if (anchor != null) {
      tag.id(anchor);
    }
    if (outlineTarget != null) {
      tag.navigationHeading(new DocumentHeading(outlineTarget, label, level));
    }
    var styled = presentation == null ? tag : presentation.apply(tag);
    return styled.children(children);
  }
}
