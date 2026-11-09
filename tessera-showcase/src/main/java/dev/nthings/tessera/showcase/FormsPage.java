package dev.nthings.tessera.showcase;

import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.components.Grid;
import dev.nthings.tessera.components.Select;
import dev.nthings.tessera.css.CssLength;

/// Text fields with validation, auto-growing textareas, switches, and customizable selects.
final class FormsPage {

  private FormsPage() {}

  static Page page() {
    return Page.ofDemos(
        "/forms",
        "Forms",
        "Forms",
        "Accessible fields that validate only after interaction, controls that size to their "
            + "content, and fully styleable selects — all native form elements.",
        fields(),
        textarea(),
        Catalog.SWITCHES.documentation(),
        select(),
        Catalog.ACTIONS.documentation());
  }

  private static Demo fields() {
    return Demo.of("fields", "Fields & validation")
        .summary(
            "Fields wire label, hint and error through compiler-resolved ids (for, "
                + "aria-describedby, aria-errormessage). Errors appear only after the user edits "
                + "the field, and a tooltip anchors to the input.")
        .features(":user-invalid", ":user-valid", "aria-errormessage", "anchor-name")
        .example(
            Grid.autoFit(
                    CssLength.rem(16),
                    Field.of("Full name", Forms.text("fullName").attr("required", ""))
                        .hint("As it appears on official documents.")
                        .error("Full name is required.")
                        .tooltip("First and last name"),
                    Field.of("Work email", Forms.email("email").attr("required", ""))
                        .hint("We send build notifications here.")
                        .error("Enter a valid email address, e.g. ada@example.com."))
                .gap(CssLength.rem(1)))
        .code(
            "Field.of(\"Work email\", Forms.email(\"email\").attr(\"required\", \"\"))",
            "    .hint(\"We send build notifications here.\")",
            "    .error(\"Enter a valid email address.\")",
            "    .tooltip(\"user@domain\");");
  }

  private static Demo textarea() {
    return Demo.of("textarea", "Auto-growing textarea")
        .summary(
            "The textarea grows with its content from a two-line minimum; no script resizes it.")
        .features("field-sizing: content", "lh unit")
        .example(Field.of("Release notes", Forms.textarea("notes", 2)))
        .code("Field.of(\"Release notes\", Forms.textarea(\"notes\", 2));");
  }

  private static Demo select() {
    return Demo.of("select", "Customizable select")
        .summary(
            "A real <select> opted into appearance: base-select: the button shows the chosen "
                + "option via <selectedcontent>, and the picker is styled like any popover.")
        .features("appearance: base-select", "::picker(select)", "<selectedcontent>")
        .example(
            Field.of(
                    "Deployment target",
                    Select.of("target")
                        .selectedOption("cloud-run", "Cloud Run")
                        .option("gke", "Kubernetes Engine")
                        .option("edge", "Edge worker"))
                .hint("Where the compiled site is deployed."))
        .code(
            "Field.of(\"Deployment target\", Select.of(\"target\")",
            "    .selectedOption(\"cloud-run\", \"Cloud Run\")",
            "    .option(\"gke\", \"Kubernetes Engine\"));");
  }
}
