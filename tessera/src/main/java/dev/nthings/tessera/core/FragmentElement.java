package dev.nthings.tessera.core;

import dev.nthings.tessera.foundation.FluentBuilder;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import java.util.ArrayList;
import java.util.List;

/// Transparent group of siblings that emits no wrapper markup, created by [Elements#fragment()].
/// Use it to return several roots, or to keep children as direct children of the surrounding
/// element.
public final class FragmentElement
    implements Element, ParentElement<FragmentElement>, FluentBuilder<FragmentElement> {

  static {
    AuthoringAccess.registerFragment(fragment -> fragment.children);
  }

  private final List<Element> children = new ArrayList<>();

  FragmentElement() {}

  @Override
  public FragmentElement self() {
    return this;
  }

  @Override
  public FragmentElement child(IntoElement child) {
    children.add(child.intoElement());
    return this;
  }

  /// A copy of this group whose children are also copies.
  FragmentElement copy() {
    var copy = new FragmentElement();
    for (var child : children) {
      copy.children.add(TagElement.detach(child));
    }
    return copy;
  }
}
