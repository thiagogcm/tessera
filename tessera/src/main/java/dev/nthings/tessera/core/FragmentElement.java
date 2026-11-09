package dev.nthings.tessera.core;

import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Transparent group of siblings that emits no wrapper markup, created by [Elements#fragment()].
/// Use it to return several roots, or to keep children as direct children of the surrounding
/// element.
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

  /// Compiler-facing: a read-only view of the children, in order.
  public List<Element> childrenList() {
    return Collections.unmodifiableList(children);
  }
}
