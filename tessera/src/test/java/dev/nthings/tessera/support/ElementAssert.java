package dev.nthings.tessera.support;

import java.util.Arrays;
import java.util.Objects;
import org.assertj.core.api.AbstractAssert;
import org.jsoup.nodes.Element;

/// AssertJ assertions for a jsoup [Element]; failures print its opening tag, not a bare
/// `expected: true`.
public final class ElementAssert extends AbstractAssert<ElementAssert, Element> {

  ElementAssert(Element actual) {
    super(actual, ElementAssert.class);
  }

  public ElementAssert hasTagName(String tagName) {
    isNotNull();
    if (!actual.tagName().equals(tagName)) {
      failWithMessage(
          "%nExpecting %s to be a <%s> element but was <%s>",
          describe(), tagName, actual.tagName());
    }
    return this;
  }

  public ElementAssert hasId(String id) {
    isNotNull();
    if (!actual.id().equals(id)) {
      failWithMessage("%nExpecting %s to have id <%s> but was <%s>", describe(), id, actual.id());
    }
    return this;
  }

  public ElementAssert hasText(String text) {
    isNotNull();
    if (!actual.text().equals(text)) {
      failWithMessage(
          "%nExpecting %s to have text <%s> but was <%s>", describe(), text, actual.text());
    }
    return this;
  }

  public ElementAssert hasAttribute(String name) {
    isNotNull();
    if (!actual.hasAttr(name)) {
      failWithMessage("%nExpecting %s to have attribute <%s>", describe(), name);
    }
    return this;
  }

  public ElementAssert hasAttribute(String name, String value) {
    hasAttribute(name);
    if (!Objects.equals(actual.attr(name), value)) {
      failWithMessage(
          "%nExpecting %s to have attribute <%s> = <%s> but was <%s>",
          describe(), name, value, actual.attr(name));
    }
    return this;
  }

  public ElementAssert doesNotHaveAttribute(String name) {
    isNotNull();
    if (actual.hasAttr(name)) {
      failWithMessage("%nExpecting %s not to have attribute <%s>", describe(), name);
    }
    return this;
  }

  public ElementAssert hasClasses(String... classNames) {
    isNotNull();
    var missing = Arrays.stream(classNames).filter(c -> !actual.hasClass(c)).toList();
    if (!missing.isEmpty()) {
      failWithMessage("%nExpecting %s to have classes %s", describe(), missing);
    }
    return this;
  }

  public ElementAssert doesNotHaveClasses(String... classNames) {
    isNotNull();
    var present = Arrays.stream(classNames).filter(actual::hasClass).toList();
    if (!present.isEmpty()) {
      failWithMessage("%nExpecting %s not to have classes %s", describe(), present);
    }
    return this;
  }

  private String describe() {
    return actual.shallowClone().outerHtml();
  }
}
