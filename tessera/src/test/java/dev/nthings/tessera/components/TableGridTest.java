package dev.nthings.tessera.components;

import static dev.nthings.tessera.components.TableGrid.Merge.BOTH;
import static dev.nthings.tessera.components.TableGrid.Merge.LEFT;
import static dev.nthings.tessera.components.TableGrid.Merge.UP;
import static dev.nthings.tessera.components.TableGrid.body;
import static dev.nthings.tessera.components.TableGrid.columnHeader;
import static dev.nthings.tessera.components.TableGrid.data;
import static dev.nthings.tessera.components.TableGrid.foot;
import static dev.nthings.tessera.components.TableGrid.head;
import static dev.nthings.tessera.components.TableGrid.row;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("TableGrid")
class TableGridTest {

  @Test
  @DisplayName("resolves a rectangular span whose middle row has no anchors")
  void resolvesSpanAcrossThreeRows() {
    var grid =
        TableGrid.of(
            body(row(data("merged"), LEFT, LEFT), row(UP, BOTH, BOTH), row(UP, BOTH, BOTH)));

    assertThat(grid.columns()).isEqualTo(3);
    assertThat(grid.cells())
        .singleElement()
        .satisfies(cell -> {
          assertThat(cell.rowSpan()).isEqualTo(3);
          assertThat(cell.columnSpan()).isEqualTo(3);
        });
    assertThat(grid.groups().getFirst().rows().get(1)).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("invalidGrids")
  @DisplayName("rejects malformed topology at construction")
  void rejectsMalformedTopology(TableGrid.Section[] sections) {
    assertThatIllegalArgumentException().isThrownBy(() -> TableGrid.of(sections));
  }

  @Test
  @DisplayName("body-local column headings label only their own group")
  void keepsBodyHeadingsLocal() {
    var grid =
        TableGrid.of(
            body(row(columnHeader("North")), row(data("12"))),
            body(row(columnHeader("South")), row(data("15"))));

    assertThat(grid.headers(grid.cells().getLast()))
        .extracting(header -> header.cell().label())
        .containsExactly("South");
  }

  private static Arguments invalid(String scenario, TableGrid.Section... sections) {
    return argumentSet(scenario, (Object) sections);
  }

  static Stream<Arguments> invalidGrids() {
    return Stream.of(
        invalid("ragged row", body(row(data("a"), data("b")), row(data("c")))),
        invalid("merge left at the left edge", body(row(LEFT, data("a")))),
        invalid("merge up at the top edge", body(row(UP))),
        invalid(
            "merge both across unrelated cells", body(row(data("a"), data("b")), row(UP, BOTH))),
        invalid("L-shaped cell", body(row(data("a"), LEFT), row(UP, data("hole")))),
        invalid("wrong horizontal continuation", body(row(data("a"), LEFT), row(UP, LEFT))),
        invalid("wrong vertical continuation", body(row(data("a"), LEFT), row(UP, UP))),
        invalid("continuation across groups", head(row(data("a"))), body(row(UP))),
        invalid("head after body", body(row(data("a"))), head(row(data("b")))),
        invalid("two footers", foot(row(data("a"))), foot(row(data("b")))));
  }
}
