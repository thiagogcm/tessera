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

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidGrids")
  @DisplayName("rejects malformed topology at construction")
  void rejectsMalformedTopology(String scenario, String error, TableGrid.Section[] sections) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TableGrid.of(sections))
        .withMessageContaining(error);
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

  static Stream<Arguments> invalidGrids() {
    return Stream.of(
        Arguments.of(
            "ragged row",
            "must have 2 slots",
            new TableGrid.Section[] {body(row(data("a"), data("b")), row(data("c")))}),
        Arguments.of(
            "left edge", "Invalid LEFT", new TableGrid.Section[] {body(row(LEFT, data("a")))}),
        Arguments.of("top edge", "Invalid UP", new TableGrid.Section[] {body(row(UP))}),
        Arguments.of(
            "unrelated neighbors",
            "Invalid BOTH",
            new TableGrid.Section[] {body(row(data("a"), data("b")), row(UP, BOTH))}),
        Arguments.of(
            "L-shaped cell",
            "Nonrectangular",
            new TableGrid.Section[] {body(row(data("a"), LEFT), row(UP, data("hole")))}),
        Arguments.of(
            "wrong horizontal continuation",
            "Invalid LEFT",
            new TableGrid.Section[] {body(row(data("a"), LEFT), row(UP, LEFT))}),
        Arguments.of(
            "wrong vertical continuation",
            "Invalid UP",
            new TableGrid.Section[] {body(row(data("a"), LEFT), row(UP, UP))}),
        Arguments.of(
            "cross-group continuation",
            "Invalid UP",
            new TableGrid.Section[] {head(row(data("a"))), body(row(UP))}),
        Arguments.of(
            "head after body",
            "Table groups",
            new TableGrid.Section[] {body(row(data("a"))), head(row(data("b")))}),
        Arguments.of(
            "duplicate footer",
            "Table groups",
            new TableGrid.Section[] {foot(row(data("a"))), foot(row(data("b")))}));
  }
}
