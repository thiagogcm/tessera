package dev.nthings.tessera.core;

import dev.nthings.tessera.css.CssDeclaration;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Interactivity;
import dev.nthings.tessera.css.StyleRefinement;
import dev.nthings.tessera.css.Styled;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Shared interactive, attribute, and container-query capabilities implemented by both {@link
 * TagElement} and {@link StatefulTagElement}.
 *
 * @param <SELF> concrete element type
 */
public interface InteractiveElement<
        SELF extends InteractiveElement<SELF> & Styled<SELF> & FluentBuilder<SELF>>
    extends Styled<SELF>, FluentBuilder<SELF> {

  Interactivity interactivity();

  Map<String, String> attributes();

  default SELF attr(String name, String value) {
    attributes().put(name, value);
    return self();
  }

  default SELF boolAttr(String name, boolean enabled) {
    if (enabled) {
      attributes().put(name, "");
    } else {
      attributes().remove(name);
    }
    return self();
  }

  default SELF ariaLabel(String label) {
    return attr("aria-label", label);
  }

  default SELF ariaHidden(boolean hidden) {
    return attr("aria-hidden", Boolean.toString(hidden));
  }

  default SELF role(String role) {
    return attr("role", role);
  }

  default SELF title(String title) {
    return attr("title", title);
  }

  default SELF hover(UnaryOperator<StyleRefinement> mutator) {
    interactivity().onHover(mutator);
    return self();
  }

  default SELF focusVisible(UnaryOperator<StyleRefinement> mutator) {
    interactivity().onFocusVisible(mutator);
    return self();
  }

  default SELF active(UnaryOperator<StyleRefinement> mutator) {
    interactivity().onActive(mutator);
    return self();
  }

  default SELF group(String groupName) {
    return attr("data-group", Fnv1a64.sanitizeIdentifier(groupName));
  }

  default SELF groupHover(String groupName, UnaryOperator<StyleRefinement> mutator) {
    interactivity().onGroupHover(Fnv1a64.sanitizeIdentifier(groupName), mutator);
    return self();
  }

  default SELF whenHas(String relativeSelector, UnaryOperator<StyleRefinement> mutator) {
    interactivity().onHas(relativeSelector, mutator);
    return self();
  }

  default SELF container(String containerName) {
    String safeName = Fnv1a64.sanitizeIdentifier(containerName);
    style().put(new CssDeclaration.ContainerName(safeName));
    style().put(new CssDeclaration.ContainerType(CssDeclaration.ContainerKind.INLINE_SIZE));
    return self();
  }

  default SELF onContainer(
      String containerName, CssLength minInlineSize, UnaryOperator<StyleRefinement> mutator) {
    interactivity()
        .onContainerMinWidth(Fnv1a64.sanitizeIdentifier(containerName), minInlineSize, mutator);
    return self();
  }

  default SELF onContainerStyle(
      String containerName,
      String customProperty,
      String expectedValue,
      UnaryOperator<StyleRefinement> mutator) {
    interactivity()
        .onContainerStyleQuery(
            Fnv1a64.sanitizeIdentifier(containerName), customProperty, expectedValue, mutator);
    return self();
  }
}
