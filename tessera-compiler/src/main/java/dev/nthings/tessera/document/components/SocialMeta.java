package dev.nthings.tessera.document.components;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.Documents;
import java.net.URI;
import java.net.URISyntaxException;
import org.jspecify.annotations.Nullable;

/// Open Graph and Twitter card tags for link previews, added to the page's `<head>`. Place one in a
/// page or a shared layout; it renders nothing in the body.
///
/// ```java
/// SocialMeta.of(title).description(description).image("/og.png").siteName("nthings.dev").article()
/// ```
///
/// It sets `og:title`, `og:type` (`website`, or `article`), and, when given, `og:description`,
/// `og:site_name` and `og:image`, with the matching `twitter:` tags and a `twitter:card` of
/// `summary_large_image` with an image or `summary` without. `og:url` is the page's absolute URL
/// when the site has a base URL. Link previews need an absolute image URL, so a site-relative image
/// resolves against the page's absolute URL; without one it reports `social-meta-url`.
@CheckReturnValue
public final class SocialMeta extends BuiltinDocumentComponent {

  private final String title;
  private final @Nullable String description;
  private final @Nullable String image;
  private final @Nullable String siteName;
  private final boolean article;

  private SocialMeta(
      String title,
      @Nullable String description,
      @Nullable String image,
      @Nullable String siteName,
      boolean article) {
    this.title = title;
    this.description = description;
    this.image = image;
    this.siteName = siteName;
    this.article = article;
  }

  /// Tags for a page previewed as `title`.
  public static SocialMeta of(String title) {
    return new SocialMeta(title, null, null, null, false);
  }

  /// The same tags with the preview's `description`.
  public SocialMeta description(String description) {
    return new SocialMeta(title, description, image, siteName, article);
  }

  /// The same tags with a preview image at `url`, absolute or site-relative such as `/og.png`.
  public SocialMeta image(String url) {
    return new SocialMeta(title, description, url, siteName, article);
  }

  /// The same tags naming the site, as `og:site_name`.
  public SocialMeta siteName(String siteName) {
    return new SocialMeta(title, description, image, siteName, article);
  }

  /// The same tags with `og:type` `article`, for a post rather than a page of the site.
  public SocialMeta article() {
    return new SocialMeta(title, description, image, siteName, true);
  }

  @Override
  DocumentContent expand(DocumentContext context) {
    var pageUrl = context.address().flatMap(address -> address.absoluteUrl()).orElse(null);
    property(context, "og:title", title);
    property(context, "og:type", article ? "article" : "website");
    if (description != null) {
      property(context, "og:description", description);
    }
    if (siteName != null) {
      property(context, "og:site_name", siteName);
    }
    if (pageUrl != null) {
      property(context, "og:url", pageUrl.toASCIIString());
    }
    var imageUrl = image == null ? null : absolute(image, pageUrl, context);
    if (imageUrl != null) {
      property(context, "og:image", imageUrl);
    }
    name(context, "twitter:card", imageUrl != null ? "summary_large_image" : "summary");
    name(context, "twitter:title", title);
    if (description != null) {
      name(context, "twitter:description", description);
    }
    if (imageUrl != null) {
      name(context, "twitter:image", imageUrl);
    }
    return Documents.fragment();
  }

  private static @Nullable String absolute(
      String url, @Nullable URI pageUrl, DocumentContext context) {
    URI parsed;
    try {
      parsed = new URI(url);
    } catch (URISyntaxException e) {
      context.report(
          "social-meta-url",
          "SocialMeta image '" + url + "' is not a valid URL",
          "Pass an absolute URL or a site-relative path such as /og.png.");
      return null;
    }
    if (parsed.isAbsolute()) {
      return parsed.toASCIIString();
    }
    if (pageUrl == null) {
      context.report(
          "social-meta-url",
          "SocialMeta image '" + url + "' is relative, and the page has no absolute URL",
          "Set a base URL on the site, or pass an absolute image URL.");
      return null;
    }
    return pageUrl.resolve(parsed).toASCIIString();
  }

  private static void property(DocumentContext context, String property, String content) {
    context.contributeHead(
        Elements.el(HtmlTag.META).attr("property", property).attr("content", content));
  }

  private static void name(DocumentContext context, String name, String content) {
    context.contributeHead(Elements.el(HtmlTag.META).attr("name", name).attr("content", content));
  }
}
