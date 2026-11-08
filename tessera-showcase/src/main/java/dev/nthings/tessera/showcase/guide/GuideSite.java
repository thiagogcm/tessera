package dev.nthings.tessera.showcase.guide;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.components.Image;
import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.components.Toc;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.FlexLayout;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.render.PageDocument;
import dev.nthings.tessera.site.SiteAsset;
import dev.nthings.tessera.site.SiteCompiler;
import dev.nthings.tessera.site.SiteDefinition;
import dev.nthings.tessera.site.SiteRoute;
import dev.nthings.tessera.site.SiteWriter;
import java.nio.file.Path;

/// Runnable companion to docs/consumer-guide.md; depends exclusively on public library APIs.
public final class GuideSite {
  private GuideSite() {}

  public static SiteDefinition createDefinition() {
    var tokens =
        new Tokens()
            .register(Tokens.ACCENT_PRIMARY.withValue(CssColor.oklch(55, 0.18, 150)))
            .register(Tokens.DENSITY.withValue(0.9))
            .register(Tokens.RADIUS_XL.withValue(CssLength.rem(1)));

    return SiteDefinition.builder()
        .tokens(tokens)
        .asset(
            SiteAsset.text(
                "/assets/logo.svg",
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 64 64\">"
                    + "<rect width=\"64\" height=\"64\" rx=\"12\" fill=\"seagreen\"/></svg>"))
        .route(
            new SiteRoute(
                "/",
                new PageDocument(
                    "My Tessera site",
                    page(
                        "My Tessera site",
                        Image.of("assets/logo.svg", "Site logo").dimensions(64, 64),
                        new MetricCard("builds", "Successful builds", "128"),
                        a("guide/?source=home#installation").text("Read the installation guide")))))
        .route(
            new SiteRoute(
                "/guide",
                new PageDocument(
                    "Guide",
                    page(
                        "Guide",
                        Toc.fromContext("contents", "On this page"),
                        Toc.section(
                            "installation",
                            "Installation",
                            p().text("Build with JDK 25 and Maven.")),
                        Toc.section(
                            "publishing",
                            "Publishing",
                            p().text("Upload the generated directory to a static host."))))))
        .build();
  }

  private static Element page(String title, IntoElement... body) {
    var search =
        SearchPalette.create("guide-search")
            .item("Pages", "Home", "/")
            .item("Guide", "Installation", "/guide/#installation")
            .item("Guide", "Publishing", "/guide/#publishing");
    return Elements.main()
        .p(Tokens.SPACE_8)
        .children(
            nav()
                .layout(
                    FlexLayout.row(Tokens.SPACE_4, FlexLayout.Align.CENTER, FlexLayout.Wrap.WRAP))
                .ariaLabel("Site navigation")
                .children(a("/").text("Home"), a("/guide/").text("Guide"), search.triggerButton()),
            h1().text(title),
            div()
                .layout(
                    FlexLayout.column(
                        Tokens.SPACE_4, FlexLayout.Align.STRETCH, FlexLayout.Wrap.NOWRAP))
                .children(body),
            search.asDeferred(10));
  }

  /// Scoped component with a stable id, styled through shared tokens.
  public record MetricCard(String id, String title, String value) implements RenderView {
    @Override
    public ElementId entityId() {
      return ElementId.of(id);
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope.root(
          s ->
              s.p(Tokens.SPACE_5)
                  .rounded(Tokens.RADIUS_XL)
                  .bg(Tokens.SURFACE_CARD)
                  .borderSubtle(Tokens.BORDER_SUBTLE));
    }

    @Override
    public Element render(RenderContext context) {
      return div().children(h2().text(title), p().text(value));
    }
  }

  public static void main(String[] args) {
    var output = Path.of(args.length == 0 ? "target/guide" : args[0]);
    SiteWriter.writeToDirectory(SiteCompiler.compile(createDefinition()), output);
    System.out.println("Wrote guide example to " + output.toAbsolutePath());
  }
}
