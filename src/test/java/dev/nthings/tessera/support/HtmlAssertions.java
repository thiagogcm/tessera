package dev.nthings.tessera.support;

import org.assertj.core.api.Assertions;
import org.jsoup.nodes.Element;

/// The single static-import source for tests: AssertJ's [Assertions] plus a typed
/// [#assertThat(Element)] for jsoup elements.
public final class HtmlAssertions extends Assertions {

  private HtmlAssertions() {}

  public static ElementAssert assertThat(Element actual) {
    return new ElementAssert(actual);
  }
}
