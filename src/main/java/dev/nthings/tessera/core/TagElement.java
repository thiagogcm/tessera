package dev.nthings.tessera.core;

import dev.nthings.tessera.css.Interactivity;
import dev.nthings.tessera.css.StyleRefinement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete stateless HTML element node backed by {@link HtmlTag}, {@link StyleRefinement}, and
 * {@link Interactivity}.
 *
 * <p>Promotes to {@link StatefulTagElement} when {@link #id(ElementId)} or {@link #id(String)} is
 * called.
 */
public final class TagElement
    implements Element,
        InteractiveElement<TagElement>,
        ParentElement<TagElement>,
        FluentBuilder<TagElement> {

  private final HtmlTag tag;
  private final StyleRefinement style = new StyleRefinement();
  private final Interactivity interactivity = new Interactivity();
  private final Map<String, String> attributes = new LinkedHashMap<>();
  private final List<Element> children = new ArrayList<>();

  public TagElement(HtmlTag tag) {
    this.tag = tag;
  }

  @Override
  public TagElement self() {
    return this;
  }

  public HtmlTag tag() {
    return tag;
  }

  @Override
  public StyleRefinement style() {
    return style;
  }

  @Override
  public Interactivity interactivity() {
    return interactivity;
  }

  @Override
  public Map<String, String> attributes() {
    return attributes;
  }

  public List<Element> childrenList() {
    return children;
  }

  @Override
  public void appendChild(Element child) {
    if (tag.isVoid()) {
      throw new IllegalStateException(
          "HTML5 void element <" + tag.tagName() + "> cannot have children");
    }
    children.add(child);
  }

  public StatefulTagElement id(ElementId elementId) {
    return new StatefulTagElement(elementId, this);
  }

  public StatefulTagElement id(String id) {
    return new StatefulTagElement(ElementId.of(id), this);
  }
}
