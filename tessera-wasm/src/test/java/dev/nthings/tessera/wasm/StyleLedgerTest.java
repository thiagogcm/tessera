package dev.nthings.tessera.wasm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Style ledger")
class StyleLedgerTest {

  @Test
  @DisplayName("shared rules are inserted once and kept while any tree uses them")
  void sharedRulesAreInsertedOnceAndKeptWhileAnyTreeUsesThem() {
    var ledger = new StyleLedger(0);

    assertThat(ledger.acquire(List.of("a", "b"))).containsExactly("a", "b");
    assertThat(ledger.acquire(List.of("b", "c"))).containsExactly("c");
    assertThat(ledger.release(List.of("a", "b"))).containsExactly("a");
    assertThat(ledger.release(List.of("b", "c"))).containsExactly("b", "c");
    assertThat(ledger.inserted()).isZero();
  }

  @Test
  @DisplayName("changing values per render keep at most the retained number of unused rules")
  void changingValuesPerRenderKeepAtMostTheRetainedNumberOfUnusedRules() {
    var ledger = new StyleLedger(3);
    var committed = List.of("layout", "width-0");
    ledger.acquire(committed);

    for (var width = 1; width <= 100; width++) {
      var next = List.of("layout", "width-" + width);
      ledger.acquire(next);
      ledger.release(committed);
      committed = next;
    }

    assertThat(ledger.inserted()).isEqualTo(2 + 3);
  }

  @Test
  @DisplayName("a retained unused rule is reused without reinsertion and evicted least recently")
  void retainedUnusedRuleIsReusedWithoutReinsertionAndEvictedLeastRecently() {
    var ledger = new StyleLedger(2);
    ledger.acquire(List.of("a", "b", "c"));
    assertThat(ledger.release(List.of("a", "b", "c"))).containsExactly("a");

    assertThat(ledger.acquire(List.of("b"))).isEmpty();
    assertThat(ledger.release(List.of("b"))).isEmpty();
    assertThat(ledger.acquire(List.of("d"))).containsExactly("d");
    assertThat(ledger.release(List.of("d"))).containsExactly("c");
  }
}
