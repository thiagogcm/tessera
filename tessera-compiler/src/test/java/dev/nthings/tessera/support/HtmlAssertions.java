package dev.nthings.tessera.support;

import org.assertj.core.api.Assertions;
import org.jsoup.nodes.Element;

/// Single static-import entry for tests: AssertJ plus a typed [#assertThat(Element)].
public final class HtmlAssertions extends Assertions {

  private HtmlAssertions() {}

  public static ElementAssert assertThat(Element actual) {
    return new ElementAssert(actual);
  }
}
