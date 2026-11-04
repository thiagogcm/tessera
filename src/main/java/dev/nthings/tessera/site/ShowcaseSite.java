package dev.nthings.tessera.site;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.footer;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.section;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.core.Elements.text;

import dev.nthings.tessera.components.Accordion;
import dev.nthings.tessera.components.Breadcrumbs;
import dev.nthings.tessera.components.Breadcrumbs.Crumb;
import dev.nthings.tessera.components.Callout;
import dev.nthings.tessera.components.Card;
import dev.nthings.tessera.components.CardGrid;
import dev.nthings.tessera.components.Carousel;
import dev.nthings.tessera.components.CodeBlock;
import dev.nthings.tessera.components.DataTable;
import dev.nthings.tessera.components.DataTable.Column;
import dev.nthings.tessera.components.DocPager;
import dev.nthings.tessera.components.Dropdown;
import dev.nthings.tessera.components.Footnotes;
import dev.nthings.tessera.components.Formatting;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.components.Grid;
import dev.nthings.tessera.components.Image;
import dev.nthings.tessera.components.Lists.BulletList;
import dev.nthings.tessera.components.Lists.DescriptionList;
import dev.nthings.tessera.components.Lists.NumberedList;
import dev.nthings.tessera.components.Lists.StepList;
import dev.nthings.tessera.components.Lists.Term;
import dev.nthings.tessera.components.Overlays;
import dev.nthings.tessera.components.ScrollProgressBar;
import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.components.SimpleTable;
import dev.nthings.tessera.components.StickyHeader;
import dev.nthings.tessera.components.Tabs;
import dev.nthings.tessera.components.Tabs.NavItem;
import dev.nthings.tessera.components.Tabs.TabPanel;
import dev.nthings.tessera.components.Toc;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.HighlightTone;
import dev.nthings.tessera.css.ScrollReveal;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.UnderlineStyle;
import dev.nthings.tessera.render.OutputMode;
import dev.nthings.tessera.render.PageDocument;
import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Multi-page flagship showcase site demonstrating Tessera's Chrome 156+ (Baseline 2026+)
 * capabilities across {@code /}, {@code /telemetry}, and {@code /typography}.
 */
public final class ShowcaseSite {

  public record ServiceMetric(
      String service, String region, String p99Latency, String throughput, String availability) {}

  record ShellLayout(
      String activePath,
      String pageHeading,
      String subtitle,
      @Nullable Breadcrumbs breadcrumbs,
      Element content,
      @Nullable DocPager pager)
      implements RenderOnce {

    public ShellLayout(
        String activePath,
        String pageHeading,
        String subtitle,
        @Nullable Breadcrumbs breadcrumbs,
        Element content) {
      this(activePath, pageHeading, subtitle, breadcrumbs, content, null);
    }

    @Override
    public Element render() {
      var inspectorDrawer =
          Overlays.drawer("inspector-drawer", "Compiler & Cascade Inspector")
              .subtitle("Live Chrome 156+ Baseline 2026 Runtime")
              .body(
                  DescriptionList.of(
                      Term.of("Cascade Order", "reset..container (7 layers)"),
                      Term.of("Scoped Isolation", "@scope ([data-view]) to ([data-view])"),
                      Term.of("Top-Layer Motion", "@starting-style + allow-discrete"),
                      Term.of("Scroll Queries", "@container scroll-state(stuck: top)")),
                  SimpleTable.create()
                      .caption("Phase Invariants")
                      .headers("Phase", "Responsibility")
                      .row("1. requestLayout", "Expand views & intern FNV-1a styles")
                      .row("2. prepaint", "Verify WCAG, Invokers & cross-route #fragments")
                      .row("3. paint", "Stream OWASP-encoded HTML5 & shared CSS"));

      var quickActions =
          Dropdown.create("quick-actions", "Quick Actions")
              .linkItem("/", "Overview & Overlays", "G O")
              .linkItem("/telemetry", "Service Telemetry", "G T")
              .linkItem("/typography", "Docs & Scroll-Spy ToC", "G D")
              .separator()
              .commandItem(
                  "open-inspector",
                  inspectorDrawer.entityId(),
                  BuiltinCommand.SHOW_MODAL,
                  "Open Compiler Inspector");

      var searchPalette =
          SearchPalette.create(
                  "site-search-palette", "Search documentation, routes, and sections...")
              .item(
                  "Pages & Guides",
                  "Overview & Architecture",
                  "/",
                  "Declarative zero-JS HTML & CSS view engine for JDK 25")
              .item(
                  "Pages & Guides",
                  "Service Telemetry",
                  "/telemetry",
                  "Container-adaptive data tables, sticky headers, and action dropdowns")
              .item(
                  "Pages & Guides",
                  "Documentation Suite",
                  "/typography",
                  "Editorial callouts, accessible code blocks, and scroll-spy ToC")
              .item(
                  "Sections & Features",
                  "Accessible Forms Suite",
                  "/typography#doc-toc-layout--form-suite",
                  "CSS Level 4 :user-invalid, HTML switch, and appearance: base-select")
              .item(
                  "Sections & Features",
                  "Semantic Callout Admonitions",
                  "/typography#doc-toc-layout--admonitions",
                  "OKLCH tinted callout boxes with accessible SVGs")
              .item(
                  "Sections & Features",
                  "Keyboard-Accessible CodeBlocks",
                  "/typography#doc-toc-layout--code-blocks",
                  "Tab-accessible pre, CSS line numbering, and diff markers")
              .item(
                  "Sections & Features",
                  "Pure CSS Scroll-Spy Navigation",
                  "/typography#doc-toc-layout--scrollspy-mechanics",
                  "scroll-target-group with :target-current tracking");

      var navTabs =
          Tabs.navTabs(
              "primary-nav",
              "Primary navigation",
              activePath,
              List.of(
                  new NavItem("/", "Overview & Overlays"),
                  new NavItem("/telemetry", "Telemetry & DataTables"),
                  new NavItem("/typography", "Docs & Scroll-Spy ToC")));

      CssLength fluidPadX = CssLength.fluid(CssLength.rem(1), CssLength.cqi(3), CssLength.rem(2));

      var stickyNav =
          new StickyHeader(
              "site-header",
              div()
                  .maxInlineSize(CssLength.rem(72))
                  .mx(CssLength.auto())
                  .px(fluidPadX)
                  .py(CssLength.rem(0.625))
                  .flex()
                  .flexWrap()
                  .itemsCenter()
                  .justifyBetween()
                  .gap(CssLength.rem(0.625))
                  .children(
                      Elements.a("/")
                          .textColor(Tokens.TEXT_PRIMARY)
                          .textDecorationNone()
                          .fontWeight(700)
                          .textSize(CssLength.rem(1.0625))
                          .whiteSpace("nowrap")
                          .text("Tessera // JDK 25")
                          .transitionKey(TransitionKey.of("site-brand", "brand-morph")),
                      navTabs,
                      div()
                          .flex()
                          .itemsCenter()
                          .gap(CssLength.rem(0.5))
                          .children(
                              searchPalette.triggerButton("site-search-btn", "Search docs..."),
                              quickActions,
                              inspectorDrawer.triggerButton("open-drawer-btn", "Inspector"),
                              inspectorDrawer.asDeferred(20),
                              searchPalette.asDeferred(30))));

      var hero =
          section()
              .flex()
              .flexCol()
              .gap(CssLength.rem(0.5))
              .py(CssLength.rem(1))
              .whenPresent(breadcrumbs, (sec, bc) -> sec.child(bc))
              .children(
                  h1().textSize(
                          CssLength.fluid(
                              CssLength.rem(1.75), CssLength.cqi(4), CssLength.rem(2.65)))
                      .textBalance()
                      .text(pageHeading),
                  p().textColor(Tokens.TEXT_MUTED)
                      .textSize(CssLength.rem(1.125))
                      .textPretty()
                      .text(subtitle));

      return div()
          .overflowAnchorNone()
          .children(
              new ScrollProgressBar("page-reading-progress"),
              stickyNav,
              Elements.main()
                  .maxInlineSize(CssLength.rem(72))
                  .mx(CssLength.auto())
                  .px(fluidPadX)
                  .py(CssLength.rem(2))
                  .flex()
                  .flexCol()
                  .gap(CssLength.rem(2.5))
                  .children(hero, content, pager != null ? pager.intoElement() : Elements.empty()),
              footer()
                  .maxInlineSize(CssLength.rem(72))
                  .mx(CssLength.auto())
                  .px(fluidPadX)
                  .py(CssLength.rem(2))
                  .borderBlockStartSubtle(Tokens.BORDER_SUBTLE)
                  .textColor(Tokens.TEXT_MUTED)
                  .textSize(CssLength.rem(0.875))
                  .text("Compiled with Tessera on JDK 25 targeting Chrome 156+ (Baseline 2026)."));
    }
  }

  private ShowcaseSite() {}

  public static SiteDefinition createDefinition() {
    return new SiteDefinition(
        new Tokens(),
        OutputMode.externalAsset("/assets"),
        List.of(
            new SiteRoute(
                "/",
                buildOverviewPage(),
                "Tessera Showcase — Overview & Architecture",
                "Declarative zero-JS HTML & CSS view engine for JDK 25",
                1),
            new SiteRoute(
                "/telemetry",
                buildTelemetryPage(),
                "Telemetry & Responsive DataTables",
                "Container-adaptive data tables, sticky headers, and action dropdowns",
                2),
            new SiteRoute(
                "/typography",
                buildTypographyPage(),
                "Documentation, Lists & Scroll-Spy ToC",
                "Editorial callouts, accessible code blocks, footnotes, and scroll-target-group navigation",
                3)),
        "http://localhost:8188");
  }

  private static PageDocument buildOverviewPage() {
    var inspectModal =
        Overlays.modal("architecture-modal", "Three-Phase Static Pipeline")
            .description(
                "Zero-JS <dialog closedby=\"any\"> with @starting-style entry/exit transitions")
            .bodyText(
                "Phase 1 expands RenderOnce and RenderView trees, Phase 2 verifies WCAG, Invoker Commands, and cross-route #fragment integrity, and Phase 3 streams HTML/CSS.")
            .closeLabel("Close Dialog");

    var cardGrid =
        new CardGrid(
            "overview-cards",
            CssLength.rem(19),
            CssLength.rem(1.25),
            Card.create("telemetry-card")
                .title("Responsive DataTables")
                .subtitle("Container-adaptive tabular layouts")
                .bodyText(
                    "Sticky row and column headers that automatically collapse into labeled cards inside narrow containers.")
                .stretchedLink("/telemetry")
                .action(
                    inspectModal
                        .triggerButton("open-arch-modal", "Inspect Pipeline")
                        .interestTooltip(
                            tip ->
                                tip.p(CssLength.rem(0.5))
                                    .bg(Tokens.SURFACE_SUBTLE)
                                    .rounded(CssLength.rem(0.375))
                                    .text("Opens native <dialog closedby=\"any\">")))
                .action(inspectModal.asDeferred(10)),
            Card.create("typography-card")
                .title("Typography, Lists & Scroll-Spy ToC")
                .subtitle("Digital publishing & scroll-target-group")
                .media(
                    Image.of("/assets/typography-hero.webp", "Typography Hero Preview")
                        .dimensions(600, 240)
                        .aspectRatio("5 / 2")
                        .transitionKey(TransitionKey.of("typography-hero", "hero-morph")))
                .bodyText(
                    "Auto-numbered footnotes with interestfor hover previews, sibling-index() step lists, and :target-current Scroll-Spy navigation.")
                .stretchedLink("/typography")
                .action(span().textColor(Tokens.TEXT_MUTED).text("Zero JavaScript")),
            Card.create("edition-choice")
                .title("Baseline 2026 Target")
                .subtitle("Chrome 156+ native runtime")
                .bodyText(
                    "Select this card to test :has(> input:checked) styling without layout shift.")
                .choiceCheckbox("baseline-opt-in", "chrome-156", true));

    var phaseTabs =
        Tabs.contentTabs(
            "compiler-phases",
            List.of(
                TabPanel.of(
                    "1. requestLayout",
                    p().textPretty()
                        .text(
                            "Expands stateless RenderOnce records and scoped RenderView boundaries using JEP 506 ScopedValue<RenderContext>, folding deterministic FNV-1a 64-bit hashes and interning ScopeStyle rules.")),
                TabPanel.of(
                    "2. prepaint",
                    p().textPretty()
                        .text(
                            "Enforces WCAG accessibility contracts, verifies every commandfor and interestfor DOM target, and validates cross-route links and #fragment anchors in parallel across Virtual Threads.")),
                TabPanel.of(
                    "3. paint",
                    p().textPretty()
                        .text(
                            "Streams HTML5 and a single deduplicated 7-layer CSS bundle (@layer reset..container) with OWASP context-sensitive encoding."))));

    var accordion =
        Accordion.exclusive("architecture-faq")
            .item(
                "Why use <details name=\"...\"> with ::details-content?",
                "Shared name attributes create mutually exclusive accordions natively, while ::details-content paired with interpolate-size: allow-keywords animates block-size from 0 to auto and preserves native 'Find in page' searchability.",
                true)
            .item(
                "How do Modal, Drawer, and Dropdown animate without JavaScript?",
                "Top-layer elements (<dialog> and [popover]) use @starting-style for entry keyframes and transition-behavior: allow-discrete on display and overlay so exit transitions complete before top-layer removal.",
                false)
            .item(
                "How does the Scroll-Spy Table of Contents track visible sections?",
                "Applying scroll-target-group: auto on the <nav> container instructs Chrome 156+ to match :target-current on the anchor link whose target section is currently in view.",
                false);

    var carousel =
        Carousel.create("platform-highlights", "Chrome 156+ Platform Capabilities")
            .slide(
                "Scroll-State Container Queries",
                "Query stuck: top on sticky headers and snapped: x on scroll-snap carousel items without scroll event listeners.")
            .slide(
                "CSS Anchor Positioning",
                "Tether popovers with flip-block / flip-inline fallbacks and smoothly slide active tab underlines using anchor(left) and anchor(right).")
            .slide(
                "Declarative Invoker Commands",
                "Open modals (command=\"show-modal\"), toggle popovers (command=\"toggle-popover\"), and trigger interestfor hover previews declaratively.");

    var interactiveGrid = Grid.autoFit(CssLength.rem(24), CssLength.rem(1.5), phaseTabs, accordion);

    var body =
        new ShellLayout(
            "/",
            "Declarative HTML & CSS View Engine for JDK 25",
            "Multi-phase static compilation backed by @layer, @scope, @container, @starting-style, and native Invoker Commands.",
            null,
            div()
                .flex()
                .flexCol()
                .gap(CssLength.rem(2.25))
                .children(cardGrid, interactiveGrid, carousel),
            DocPager.of(
                null, new DocPager.PageRef("/telemetry", "Telemetry & Responsive DataTables")));

    return new PageDocument("Tessera — Overview", body);
  }

  private static PageDocument buildTelemetryPage() {
    var metrics =
        List.of(
            new ServiceMetric("gateway-edge", "us-east-1", "3.4 ms", "94,200 req/s", "99.99%"),
            new ServiceMetric(
                "compiler-worker", "eu-central-1", "8.1 ms", "41,800 req/s", "99.98%"),
            new ServiceMetric(
                "asset-bundler", "ap-northeast-1", "6.2 ms", "52,100 req/s", "99.97%"),
            new ServiceMetric("audit-stream", "us-west-2", "11.9 ms", "29,400 req/s", "99.95%"));

    var dataTable =
        new DataTable<>(
            "service-slo",
            "Regional Service SLO Telemetry",
            List.of(
                Column.rowHeader("Service", r -> text(r.service())),
                Column.text("Region", ServiceMetric::region),
                Column.numeric("P99 Latency", ServiceMetric::p99Latency),
                Column.numeric("Throughput", ServiceMetric::throughput),
                Column.numeric("Availability", ServiceMetric::availability)),
            metrics);

    var bentoSummary =
        Grid.columns(
            3,
            CssLength.rem(1),
            Grid.cell(
                    2,
                    1,
                    div()
                        .p(CssLength.rem(1.25))
                        .rounded(CssLength.rem(0.75))
                        .bg(Tokens.SURFACE_CARD)
                        .borderSubtle(Tokens.BORDER_SUBTLE)
                        .scrollReveal(ScrollReveal.FADE_UP)
                        .staggerBySiblingIndex(60)
                        .children(
                            h2().textSize(CssLength.rem(1.125)).text("Global Fleet Health"),
                            p().textColor(Tokens.TEXT_MUTED)
                                .text("All 4 regional clusters operating within error budget.")))
                .intoElement(),
            Grid.cell(
                    1,
                    1,
                    SimpleTable.create()
                        .caption("Cascade Budget")
                        .headers("Layer", "Rules")
                        .row("components", "Deduplicated")
                        .row("utilities", "FNV-1a hashed"))
                .scrollReveal(ScrollReveal.FADE_UP)
                .staggerBySiblingIndex(60));

    var breadcrumbs =
        Breadcrumbs.of(
            "telemetry-crumbs",
            Crumb.of("/", "Tessera"),
            Crumb.of("/telemetry", "Telemetry & DataTables"));

    var body =
        new ShellLayout(
            "/telemetry",
            "Telemetry & Responsive DataTables",
            "Resize the viewport or container below 36rem to observe the @container stacked-card transformation.",
            breadcrumbs,
            div().flex().flexCol().gap(CssLength.rem(2)).children(dataTable, bentoSummary),
            DocPager.of(
                new DocPager.PageRef("/", "Overview & Architecture"),
                new DocPager.PageRef("/typography", "Documentation, Lists & Scroll-Spy ToC")));

    return new PageDocument("Tessera — Telemetry", body);
  }

  private static PageDocument buildTypographyPage() {
    var heroImage =
        Image.hero("/assets/typography-hero.webp", "Tessera Documentation & Editorial Architecture")
            .dimensions(1200, 480)
            .aspectRatio("5 / 2")
            .transitionKey(TransitionKey.of("typography-hero", "hero-morph"))
            .style(
                s ->
                    s.rounded(CssLength.rem(0.75))
                        .borderSubtle(Tokens.BORDER_SUBTLE)
                        .overflowClip());

    var admonitionsSection =
        Toc.section(
            "admonitions",
            "Editorial Callouts & Admonitions",
            div()
                .flex()
                .flexCol()
                .gap(CssLength.rem(1))
                .children(
                    Callout.note(
                        "Tessera compiles statically to zero-runtime HTML5 and CSS Level 4/5. No client-side JS hydrate pass is executed."),
                    Callout.tip(
                        "Chrome 156+ natively animates ::details-content when interpolate-size: allow-keywords is active."),
                    Callout.important(
                        "All <img> elements require explicit width/height or CSS aspect-ratio to eliminate Cumulative Layout Shift (CLS)."),
                    Callout.warning(
                        "JDK preview features are disallowed in production builds for long-term ABI stability."),
                    Callout.caution(
                        "Destructive actions should always declare dialog closedby=\"none\" to prevent accidental dismissal.")));

    var codeBlocksSection =
        Toc.section(
            "code-blocks",
            "Accessible Keyboard-Scrollable Code Blocks",
            div()
                .flex()
                .flexCol()
                .gap(CssLength.rem(1))
                .children(
                    p().text(
                            "Scrollable <pre tabindex=\"0\"> with CSS counter-increment line numbering, diff markers, and line highlighting:"),
                    CodeBlock.ofLines(
                            "package dev.nthings.tessera.site;",
                            "",
                            "// Three-phase static compiler pipeline",
                            "public class ShowcaseSite {",
                            "+   // Phase 1: Layout expansion & token hoisting",
                            "+   PreparedPage prepared = PageCompiler.prepare(document);",
                            "-   // Legacy single-pass unvalidated rendering",
                            "-   String rawHtml = LegacyEngine.render(document);",
                            "    CompiledSite site = SiteCompiler.compile(definition);",
                            "}")
                        .filename("ShowcaseSite.java")
                        .language("java")
                        .diffAutoDetect(true)
                        .highlightLines(4, 9)));

    var formSuiteSection =
        Toc.section(
            "form-suite",
            "Accessible Forms & Form Controls",
            div()
                .p(CssLength.rem(1.5))
                .rounded(CssLength.rem(0.75))
                .bg(Tokens.SURFACE_CARD)
                .borderSubtle(Tokens.BORDER_SUBTLE)
                .flex()
                .flexCol()
                .gap(CssLength.rem(1.25))
                .children(
                    p().textColor(Tokens.TEXT_MUTED)
                        .textSize(CssLength.rem(0.95))
                        .textPretty()
                        .text(
                            "Tessera's Form Controls Suite demonstrates CSS Level 4 :user-invalid / :user-valid validation (suppressing premature errors before blur), HTML <input type=\"checkbox\" switch> toggles, Chrome 156+ appearance: base-select custom styling, and CSS Anchor Positioning tooltips."),
                    Elements.form()
                        .flex()
                        .flexCol()
                        .gap(CssLength.rem(1))
                        .children(
                            div()
                                .flex()
                                .flexWrap()
                                .gap(CssLength.rem(1))
                                .children(
                                    div()
                                        .flexGrow(1)
                                        .child(
                                            Forms.field("Full Name", Forms.text("fullName"))
                                                .id("input-fullname")
                                                .hint(
                                                    "Your full legal name as it appears on official documents.")
                                                .error("Full name is required.")
                                                .anchorTooltip("Format: First Last")),
                                    div()
                                        .flexGrow(1)
                                        .child(
                                            Forms.field("Work Email", Forms.email("email"))
                                                .id("input-email")
                                                .hint(
                                                    "We will send deployment telemetry and build notifications.")
                                                .error(
                                                    "Please enter a valid work email address (e.g., user@company.com).")
                                                .anchorTooltip("Format: user@domain.com"))),
                            div()
                                .flex()
                                .flexWrap()
                                .gap(CssLength.rem(1))
                                .children(
                                    div()
                                        .flexGrow(1)
                                        .child(
                                            Forms.field(
                                                    "Target Deployment Platform",
                                                    Forms.selectControl("targetPlatform")
                                                        .id("select-platform")
                                                        .option(
                                                            "cloud-run",
                                                            "Google Cloud Run (Serverless Container)",
                                                            true)
                                                        .option(
                                                            "gke",
                                                            "Google Kubernetes Engine (GKE Autopilot)")
                                                        .option(
                                                            "bare-metal", "Edge On-Premise Worker"))
                                                .hint(
                                                    "Select target orchestration runtime with appearance: base-select.")),
                                    div()
                                        .flexGrow(1)
                                        .flex()
                                        .flexCol()
                                        .justifyCenter()
                                        .gap(CssLength.rem(0.75))
                                        .p(CssLength.rem(0.875))
                                        .rounded(CssLength.rem(0.5))
                                        .bg(Tokens.SURFACE_SUBTLE)
                                        .borderSubtle(Tokens.BORDER_SUBTLE)
                                        .children(
                                            Forms.switchField(
                                                "Enable JIT Telemetry Stream",
                                                "stream_telemetry",
                                                true),
                                            Forms.switchField(
                                                "Strict WCAG 2.2 AAA Enforcement",
                                                "wcag_strict",
                                                true))))));

    var inlineFormattingSection =
        Toc.section(
            "inline-formatting",
            "Inline Formatting & Interactive Citations",
            div()
                .p(CssLength.rem(1.5))
                .rounded(CssLength.rem(0.75))
                .bg(Tokens.SURFACE_CARD)
                .borderSubtle(Tokens.BORDER_SUBTLE)
                .child(
                    p().textPretty()
                        .children(
                            text("Tessera combines "),
                            Formatting.highlight("OKLCH adaptive highlights", HighlightTone.ACCENT),
                            text(", "),
                            Formatting.underline(
                                "CSS Text Decoration Level 4 wavy underlines",
                                UnderlineStyle.WAVY,
                                Tokens.STATUS_WARNING),
                            text(", "),
                            Formatting.strikethrough("legacy polyfill shims"),
                            text(", diff markers ("),
                            Formatting.deleted("String viewScopeRule()"),
                            text(" -> "),
                            Formatting.inserted("void configureScope(ScopeStyle)"),
                            text("), inline code "),
                            Formatting.inlineCode("ScopedValue<RenderContext>"),
                            text(", and keyboard shortcuts like "),
                            Formatting.keybinding("Ctrl+Shift+P"),
                            Footnotes.ref(
                                "scoped-values",
                                "JEP 506 Scoped Values propagate RenderContext lexically without parameter drilling."),
                            text(" alongside native CSS Cascade Layers"),
                            Footnotes.ref(
                                "cascade-layers",
                                "The 7-layer cascade guarantees @layer interactive and @layer container override @layer utilities."))));

    var stepListSection =
        Toc.section(
            "step-progression",
            "Sibling-Index Step Progression",
            div()
                .p(CssLength.rem(1.25))
                .rounded(CssLength.rem(0.75))
                .bg(Tokens.SURFACE_CARD)
                .borderSubtle(Tokens.BORDER_SUBTLE)
                .child(
                    StepList.ofStrings(
                        "pipeline-steps",
                        "Phase 1: requestLayout (Expand & Intern)",
                        "Phase 2: prepaint (WCAG & Link Integrity)",
                        "Phase 3: paint (Streaming OWASP Encode)")));

    var specsSubSection =
        Toc.subsection(
            "platform-specs",
            "Platform & Compiler Specifications",
            div()
                .p(CssLength.rem(1.25))
                .rounded(CssLength.rem(0.75))
                .bg(Tokens.SURFACE_CARD)
                .borderSubtle(Tokens.BORDER_SUBTLE)
                .flex()
                .flexCol()
                .gap(CssLength.rem(0.75))
                .children(
                    DescriptionList.of(
                        Term.of("Java Baseline", "JDK 25 LTS"),
                        Term.of("Browser Target", "Chrome 156+"),
                        Term.of("Nullness Contract", "JSpecify @NullMarked + NullAway")),
                    BulletList.ofStrings("Content-addressed @scope", "Shared CSS site bundle"),
                    NumberedList.ofStrings("Zero preview flags", "Zero warning suppressions")));

    var scrollSpySection =
        Toc.section(
            "scrollspy-mechanics",
            "Pure CSS Scroll-Spy Navigation",
            div()
                .p(CssLength.rem(1.25))
                .rounded(CssLength.rem(0.75))
                .bg(Tokens.SURFACE_CARD)
                .borderSubtle(Tokens.BORDER_SUBTLE)
                .child(
                    p().textPretty()
                        .text(
                            "The sticky 'On This Page' sidebar on the right sets scroll-target-group: auto on its <nav> container and styles a:target-current with a high-contrast accent border, bold weight, and OKLCH surface tint as sections scroll through the viewport.")));

    var docLayout =
        Toc.docLayout(
            "doc-toc",
            "On This Page",
            admonitionsSection,
            codeBlocksSection,
            formSuiteSection,
            inlineFormattingSection,
            stepListSection,
            specsSubSection,
            scrollSpySection,
            Footnotes.section());

    var breadcrumbs =
        Breadcrumbs.of(
            "typography-crumbs",
            Crumb.of("/", "Tessera"),
            Crumb.of("/typography", "Docs & Scroll-Spy ToC"));

    var body =
        new ShellLayout(
            "/typography",
            "Documentation, Lists & Scroll-Spy ToC",
            "Scroll through the sections to observe :target-current Scroll-Spy tracking, test Callout tones, or scroll keyboard-focused CodeBlocks.",
            breadcrumbs,
            div()
                .flex()
                .flexCol()
                .gap(CssLength.rem(2))
                .children(heroImage, docLayout.intoElement()),
            DocPager.of(
                new DocPager.PageRef("/telemetry", "Telemetry & Responsive DataTables"), null));

    return new PageDocument("Tessera — Documentation & Typography", body);
  }

  public static void main(String[] args) throws InterruptedException {
    Path outputDir = Path.of("target/site");
    boolean serve = false;
    int port = 8080;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--output" -> {
          if (i + 1 < args.length) {
            outputDir = Path.of(args[++i]);
          }
        }
        case "--serve" -> {
          serve = true;
          if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
            port = Integer.parseInt(args[++i]);
          }
        }
        case "--port" -> {
          if (i + 1 < args.length) {
            port = Integer.parseInt(args[++i]);
          }
        }
        default -> {}
      }
    }

    SiteCompiler.CompiledSite compiled = SiteCompiler.compile(createDefinition());
    SiteCompiler.writeToDirectory(compiled, outputDir);
    System.out.println(
        "Compiled "
            + compiled.pagesByRoute().size()
            + " routes + shared stylesheet ("
            + compiled.sharedCssPath()
            + ") to "
            + outputDir.toAbsolutePath());

    if (serve) {
      try (var server = SitePreviewServer.start(compiled, port)) {
        System.out.println("Serving Tessera showcase at " + server.baseUrl() + " (Ctrl+C to stop)");
        Thread.currentThread().join();
      }
    }
  }
}
