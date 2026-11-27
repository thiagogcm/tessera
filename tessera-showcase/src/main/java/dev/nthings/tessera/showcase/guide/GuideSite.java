package dev.nthings.tessera.showcase.guide;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.compiler.Asset;
import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.compiler.SiteDefinition;
import dev.nthings.tessera.compiler.SiteRoute;
import dev.nthings.tessera.compiler.SiteWriter;
import dev.nthings.tessera.components.Image;
import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.components.Section;
import dev.nthings.tessera.core.DocumentContent;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.CssNumber;
import dev.nthings.tessera.css.FlexLayout;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.Documents;
import dev.nthings.tessera.document.components.TableOfContents;
import java.nio.file.Path;
import java.util.List;

/// Runnable two-page site example using only public library APIs.
public final class GuideSite {
  private GuideSite() {}

  public static Theme theme() {
    return Theme.defaults()
        .with(
            Tokens.ACCENT_PRIMARY,
            CssColor.lightDark(CssColor.oklch(50, 0.16, 150), CssColor.oklch(72, 0.16, 150)))
        .with(Tokens.DENSITY, CssNumber.of(0.9))
        .with(Tokens.RADIUS_XL, CssLength.rem(1));
  }

  public static SiteDefinition createDefinition() {
    return SiteDefinition.of(
            SiteRoute.of(
                "/",
                PageDocument.of(
                    "My Tessera site",
                    page(
                        "My Tessera site",
                        Image.of("assets/logo.svg", "Site logo").dimensions(64, 64),
                        new MetricCard("builds", "Successful builds", "128"),
                        a("guide/?source=home#installation", "Read the installation guide")))),
            SiteRoute.of(
                "/guide",
                PageDocument.of(
                    "Guide",
                    page(
                        "Guide",
                        TableOfContents.of("On this page"),
                        Section.of("installation", "Installation")
                            .child(p("Build with JDK 25 and Maven.")),
                        Section.of("publishing", "Publishing")
                            .child(p("Upload the generated directory to a static host."))))))
        .withAssets(
            List.of(
                Asset.text(
                    "/assets/logo.svg",
                    "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 64 64\">"
                        + "<rect width=\"64\" height=\"64\" rx=\"12\" fill=\"seagreen\"/></svg>")));
  }

  private static DocumentContent page(String title, DocumentContent... body) {
    var search =
        SearchPalette.of()
            .scopeId("guide-search")
            .item("Pages", "/", "Home")
            .item("Guide", "/guide/#installation", "Installation")
            .item("Guide", "/guide/#publishing", "Publishing");
    return DocumentElement.of(
            Elements.main()
                .p(Tokens.SPACE_8)
                .children(
                    nav()
                        .layout(
                            FlexLayout.row(
                                Tokens.SPACE_4, FlexLayout.Align.CENTER, FlexLayout.Wrap.WRAP))
                        .ariaLabel("Site navigation")
                        .children(
                            a("/").text("Home"),
                            a("/guide/").text("Guide"),
                            search.triggerButton()),
                    h1().text(title)))
        .child(
            DocumentElement.of(
                    div()
                        .layout(
                            FlexLayout.column(
                                Tokens.SPACE_4, FlexLayout.Align.STRETCH, FlexLayout.Wrap.NOWRAP)))
                .children(body))
        .child(Documents.deferred(search, 10));
  }

  /// Scoped component with a stable id, styled through shared tokens.
  public record MetricCard(String id, String title, String value) implements Scope {
    @Override
    public String scopeId() {
      return id;
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope.root(s ->
          s.p(Tokens.SPACE_5)
              .rounded(Tokens.RADIUS_XL)
              .bg(Tokens.SURFACE_CARD)
              .border(Border.hairline(Tokens.BORDER_SUBTLE)));
    }

    @Override
    public TagElement expand(ViewContext context) {
      return div().children(h2().text(title), p().text(value));
    }
  }

  public static void main(String[] args) {
    var output = Path.of(args.length == 0 ? "target/guide" : args[0]);
    SiteWriter.writeToDirectory(
        Compiler.forSite().withTheme(theme()).compile(createDefinition()), output);
    System.out.println("Wrote guide example to " + output.toAbsolutePath());
  }
}
