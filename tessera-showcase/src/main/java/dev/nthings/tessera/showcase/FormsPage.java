package dev.nthings.tessera.showcase;

import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.components.Grid;
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
                CssLength.rem(1),
                Forms.field("Full name", Forms.text("fullName").attr("required", ""))
                    .hint("As it appears on official documents.")
                    .error("Full name is required.")
                    .anchorTooltip("First and last name"),
                Forms.field("Work email", Forms.email("email").attr("required", ""))
                    .hint("We send build notifications here.")
                    .error("Enter a valid email address, e.g. ada@example.com.")))
        .code(
            "Forms.field(\"Work email\", Forms.email(\"email\").attr(\"required\", \"\"))",
            "    .hint(\"We send build notifications here.\")",
            "    .error(\"Enter a valid email address.\")",
            "    .anchorTooltip(\"user@domain\");");
  }

  private static Demo textarea() {
    return Demo.of("textarea", "Auto-growing textarea")
        .summary(
            "The textarea grows with its content from a two-line minimum; no script resizes it.")
        .features("field-sizing: content", "lh unit")
        .example(Forms.field("Release notes", Forms.textareaControl("notes", 2)))
        .code("Forms.field(\"Release notes\", Forms.textareaControl(\"notes\", 2));");
  }

  private static Demo select() {
    return Demo.of("select", "Customizable select")
        .summary(
            "A real <select> opted into appearance: base-select: the button shows the chosen "
                + "option via <selectedcontent>, and the picker is styled like any popover.")
        .features("appearance: base-select", "::picker(select)", "<selectedcontent>")
        .example(
            Forms.field(
                    "Deployment target",
                    Forms.selectControl("target")
                        .option("cloud-run", "Cloud Run", true)
                        .option("gke", "Kubernetes Engine")
                        .option("edge", "Edge worker"))
                .hint("Where the compiled site is deployed."))
        .code(
            "Forms.field(\"Deployment target\", Forms.selectControl(\"target\")",
            "    .option(\"cloud-run\", \"Cloud Run\", true)",
            "    .option(\"gke\", \"Kubernetes Engine\"));");
  }
}
