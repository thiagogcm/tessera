package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.deferred;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.compiler.SiteRoute;
import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.components.IconButton;
import dev.nthings.tessera.components.Modal;
import dev.nthings.tessera.components.Size;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.PopoverMode;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.FlexLayout;
import dev.nthings.tessera.css.Overflow;
import dev.nthings.tessera.css.Tone;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/// Named component cases shared by showcase pages and browser fixtures.
public final class Catalog {

  public enum Appearance {
    LIGHT("light"),
    DARK("dark");

    private final String wire;

    Appearance(String wire) {
      this.wire = wire;
    }

    /// Path segment and `color-scheme` value.
    public String wire() {
      return wire;
    }
  }

  /// Compact is `0.8`; regular leaves the density token at `1`.
  public enum Density {
    REGULAR("regular", "1"),
    COMPACT("compact", "0.8");

    private final String wire;
    private final String value;

    Density(String wire, String value) {
      this.wire = wire;
      this.value = value;
    }

    public String wire() {
      return wire;
    }

    String value() {
      return value;
    }
  }

  static final Story ACTIONS = actions();
  static final Story MODAL = modal();
  static final Story POPOVER = popover();
  static final Story SWITCHES = switches();
  static final Story OVERFLOW = overflow();

  private static final List<Story> STORIES = List.of(ACTIONS, MODAL, POPOVER, SWITCHES, OVERFLOW);

  private Catalog() {}

  /// The viewport is the browser window, so it is not part of the path.
  public static String casePath(
      String storyId, String variant, Appearance appearance, Density density) {
    return "/stories/" + storyId + "/" + variant + "/" + appearance.wire() + "/" + density.wire();
  }

  /// Light, regular-density path: the default view linked from docs.
  public static String casePath(String storyId, String variant) {
    return casePath(storyId, variant, Appearance.LIGHT, Density.REGULAR);
  }

  static List<SiteRoute> routes() {
    var routes = new ArrayList<SiteRoute>();
    for (var story : STORIES) {
      for (var variant : story.variants()) {
        for (var appearance : Appearance.values()) {
          for (var density : Density.values()) {
            var heading = story.heading(variant);
            routes.add(
                new SiteRoute(
                    casePath(story.id(), variant.name(), appearance, density),
                    new PageDocument(
                        "Tessera — " + heading, new StoryPage(story, variant, appearance, density)),
                    heading,
                    variant.label()));
          }
        }
      }
    }
    return routes;
  }

  private static Story actions() {
    return new Story(
        "actions",
        "Actions",
        "Native buttons whose tone, surface, and size stay separate from disabled and busy.",
        List.of("type=\"button\"", "disabled", "aria-busy", ":focus-visible"),
        List.of(
            "Button.of(\"Save\");",
            "Button.of(\"Delete\").tone(Tone.DANGER).disabled();",
            "Button.of(\"Publishing\").busy();",
            "Button.of(\"Edit\").size(Size.SM);",
            "IconButton.of(\"Refresh\", span().text(\"↻\"));"),
        List.of(
            variant("primary", "Primary", true, Catalog::primary),
            variant("disabled", "Disabled", true, Catalog::disabled),
            variant("busy", "Busy", true, Catalog::busy),
            variant("compact", "Compact", true, Catalog::compact),
            variant("icon", "Icon", true, Catalog::icon)));
  }

  private static Story modal() {
    return new Story(
        "modal",
        "Modal dialog",
        "A modal <dialog> with light dismiss. The trigger's commandfor is resolved by the "
            + "compiler to the dialog's scoped id.",
        List.of(
            "command=\"show-modal\"", "<dialog closedby=\"any\">", "@starting-style", "::backdrop"),
        List.of(
            "var modal = Modal.of(\"Publish release?\").id(\"publish\").closeLabel(\"Got it\");",
            "Button.of(\"Open modal\").command(modal.dialogRef(), BuiltinCommand.SHOW_MODAL);",
            "Elements.deferred(modal, 10);"),
        List.of(
            variant("closed", "Closed", true, Catalog::closedModal),
            variant("open", "Open", false, Catalog::openModal)));
  }

  private static Story popover() {
    return new Story(
        "popover",
        "Popover",
        "Any element can become a light-dismiss popover. The button references it through an "
            + "ElementRef, so the id link is checked at compile time.",
        List.of("popover=\"auto\"", "command=\"toggle-popover\"", "ElementRef"),
        List.of(
            "var panel = ElementRef.scoped(\"panel\");",
            "Button.of(\"Toggle popover\").command(panel, BuiltinCommand.TOGGLE_POPOVER);",
            "div().id(panel).popover(PopoverMode.AUTO).text(\"…\");"),
        List.of(variant("toggle", "Toggle", true, Catalog::togglePopover)));
  }

  private static Story switches() {
    return new Story(
        "switches",
        "Switch",
        "A checkbox with role=\"switch\", drawn as a track and sliding thumb with "
            + "appearance: none and a :checked state.",
        List.of("role=\"switch\"", "appearance: none", ":checked"),
        List.of(
            "Forms.switchField(\"Weekly digest\", \"notify_digest\", false);",
            "Forms.switchField(\"Weekly digest\", \"notify_digest\", true);"),
        List.of(
            variant("off", "Off", true, () -> digest(false)),
            variant("on", "On", false, () -> digest(true))));
  }

  private static Story overflow() {
    return new Story(
        "overflow",
        "Overflowing actions",
        "A wide row scrolls inside its own region, including from the keyboard, while the "
            + "document stays within the viewport.",
        List.of("overflow-x: auto", "min-inline-size: 0"),
        List.of(
            "div().overflowX(Overflow.AUTO).minInlineSize(CssLength.px(0))",
            "    .child(div().flex().minInlineSize(CssLength.rem(48))",
            "        .children(Button.of(\"Save\"), Button.of(\"Archive\")));"),
        List.of(variant("row", "Wide row", true, Catalog::overflowRow)));
  }

  private static Story.Variant variant(
      String name, String label, boolean inDocs, Supplier<IntoElement> content) {
    return new Story.Variant(name, label, inDocs, content);
  }

  private static IntoElement primary() {
    return Button.of("Save");
  }

  private static IntoElement disabled() {
    return Button.of("Delete").tone(Tone.DANGER).disabled();
  }

  private static IntoElement busy() {
    return Button.of("Publishing").busy();
  }

  private static IntoElement compact() {
    return Button.of("Edit").size(Size.SM);
  }

  private static IntoElement icon() {
    return IconButton.of("Refresh", span().text("↻"));
  }

  private static IntoElement closedModal() {
    var modal =
        Modal.of("Publish release?")
            .id("publish")
            .description("A native <dialog> opened with command=\"show-modal\".")
            .bodyText(
                "Press Escape or click the backdrop to dismiss — closedby=\"any\" handles both.")
            .closeLabel("Got it");
    return div()
        .layout(FlexLayout.row(CssLength.rem(0.75), FlexLayout.Align.CENTER, FlexLayout.Wrap.WRAP))
        .children(
            Button.of("Open modal").command(modal.dialogRef(), BuiltinCommand.SHOW_MODAL),
            deferred(modal, 10));
  }

  private static IntoElement openModal() {
    return Modal.of("Publish release?")
        .id("publish")
        .description("Shown open, without a second trigger.")
        .bodyText(
            "This note is long enough to wrap onto several lines in a narrow window. The dialog "
                + "stays inside the viewport, and the document does not gain a horizontal "
                + "scrollbar.")
        .closeLabel("Got it")
        .opened();
  }

  private static IntoElement togglePopover() {
    var panel = ElementRef.scoped("panel");
    return div()
        .layout(
            FlexLayout.column(
                CssLength.rem(0.75), FlexLayout.Align.STRETCH, FlexLayout.Wrap.NOWRAP))
        .children(
            Button.of("Toggle popover").command(panel, BuiltinCommand.TOGGLE_POPOVER),
            Demo.surface(
                    div().id(panel).popover(PopoverMode.AUTO).p(CssLength.rem(1)),
                    CssLength.rem(0.75))
                .text("Click outside or press Escape to close."));
  }

  private static IntoElement digest(boolean checked) {
    return Forms.switchField("Weekly digest", "notify_digest", checked);
  }

  private static IntoElement overflowRow() {
    return div()
        .attr("class", "story-overflow")
        .overflowX(Overflow.AUTO)
        .minInlineSize(CssLength.px(0))
        .maxInlineSize(CssLength.pct(100))
        .inlineSize(CssLength.pct(100))
        .child(
            div()
                .layout(
                    FlexLayout.row(
                        CssLength.rem(0.75), FlexLayout.Align.CENTER, FlexLayout.Wrap.NOWRAP))
                .minInlineSize(CssLength.rem(48))
                .children(
                    Button.of("Save"),
                    Button.of("Review"),
                    Button.of("Publish"),
                    Button.of("Schedule"),
                    Button.of("Archive")));
  }
}
