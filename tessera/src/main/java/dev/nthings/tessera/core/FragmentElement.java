package dev.nthings.tessera.core;

import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.ArrayList;
import java.util.List;

/// Transparent group of sibling elements that emits no wrapper markup.
///
/// Use a fragment when a component needs to return several roots, or when a child list must stay a
/// direct child of the surrounding element for layout or content-model reasons.
public final class FragmentElement
    implements Element, ParentElement<FragmentElement>, FluentBuilder<FragmentElement> {

  private final List<Element> children = new ArrayList<>();

  @Override
  public FragmentElement self() {
    return this;
  }

  @Override
  public void appendChild(Element child) {
    children.add(child);
  }

  public List<Element> childrenList() {
    return children;
  }
}
