package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.text;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.components.DataTable.Column;
import dev.nthings.tessera.support.RenderedPage;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DataTable")
class DataTableTest {

  record ServiceRow(String service, String region) {}

  private static RenderedPage page;

  @BeforeAll
  static void renderTable() {
    var table =
        new DataTable<>(
            "services",
            "Service telemetry",
            List.of(
                Column.rowHeader("Service", r -> text(r.service())),
                Column.text("Region \"AZ\"", ServiceRow::region)),
            List.of(new ServiceRow("auth-edge", "us-east-1"), new ServiceRow("ledger", "eu-1")));
    page = renderMain(table);
  }

  @Test
  @DisplayName("escapes headers reused as stacked-cell labels in CSS")
  void escapesHeadersUsedAsStackedCellLabels() {
    assertThat(page.css()).contains("--label-2:").doesNotContain("\"Region \"AZ\"\"");
  }

  @Test
  @DisplayName("labels stacked data cells but not the row-header column")
  void labelsStackedDataCellsButNotRowHeaders() {
    assertThat(page.css())
        .contains("td:nth-child(2)::before")
        .doesNotContain("td:nth-child(1)::before");
  }

  @Test
  @DisplayName("is a focusable scroll region labelled by its caption")
  void isFocusableScrollRegionLabelledByCaption() {
    var region = page.element("div[role=region][tabindex=0]");

    assertThat(region).hasAttribute("aria-labelledby", region.selectFirst("caption").id());
  }

  @Test
  @DisplayName("renders row headers as <th scope=row>")
  void rendersRowHeadersAsTh() {
    assertThat(page.elements("tbody th[scope=row]"))
        .extracting(e -> e.text())
        .containsExactly("auth-edge", "ledger");
  }
}
