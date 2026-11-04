package dev.nthings.tessera.site;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.nthings.tessera.render.PageCompiler;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/// Lightweight, zero-dependency HTTP server backed by JDK [HttpServer] and Virtual Threads for
/// previewing and evaluating a [SiteCompiler.CompiledSite] in Chrome 156+.
public final class SitePreviewServer implements AutoCloseable {

  private final HttpServer server;

  private SitePreviewServer(HttpServer server) {
    this.server = server;
  }

  public static SitePreviewServer start(SiteCompiler.CompiledSite site, int port) {
    try {
      var httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
      httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
      httpServer.createContext("/", exchange -> handleRequest(site, exchange));
      httpServer.start();
      return new SitePreviewServer(httpServer);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public int port() {
    return server.getAddress().getPort();
  }

  public String baseUrl() {
    return "http://localhost:" + port();
  }

  @Override
  public void close() {
    server.stop(0);
  }

  private static void handleRequest(SiteCompiler.CompiledSite site, HttpExchange exchange)
      throws IOException {
    String requestPath = exchange.getRequestURI().getPath();
    if (requestPath == null || requestPath.isBlank()) {
      requestPath = "/";
    }

    String sharedCssPath = site.sharedCssPath();
    if (sharedCssPath != null && sharedCssPath.equals(requestPath)) {
      sendResponse(exchange, 200, "text/css; charset=utf-8", site.sharedCssContent());
      return;
    }

    String sitemap = site.sitemapXml();
    if ("/sitemap.xml".equals(requestPath) && sitemap != null) {
      sendResponse(exchange, 200, "application/xml; charset=utf-8", sitemap);
      return;
    }

    String searchIndex = site.searchIndexJson();
    if ("/search-index.json".equals(requestPath) && searchIndex != null) {
      sendResponse(exchange, 200, "application/json; charset=utf-8", searchIndex);
      return;
    }

    SiteAsset asset = site.asset(requestPath);
    if (asset != null) {
      sendBytes(exchange, 200, asset.contentType(), asset.content());
      return;
    }

    String withoutIndex =
        requestPath.endsWith("/index.html")
            ? requestPath.substring(0, requestPath.length() - "/index.html".length())
            : requestPath;
    String normalized = SiteRoute.normalizeRoutePath(withoutIndex.isEmpty() ? "/" : withoutIndex);

    PageCompiler.CompiledPage page = site.pagesByRoute().get(normalized);
    if (page != null) {
      sendResponse(exchange, 200, "text/html; charset=utf-8", page.html());
      return;
    }

    sendResponse(exchange, 404, "text/plain; charset=utf-8", "404 Not Found: " + requestPath);
  }

  private static void sendResponse(
      HttpExchange exchange, int status, String contentType, String body) throws IOException {
    sendBytes(exchange, status, contentType, body.getBytes(StandardCharsets.UTF_8));
  }

  private static void sendBytes(HttpExchange exchange, int status, String contentType, byte[] bytes)
      throws IOException {
    exchange.getResponseHeaders().set("Content-Type", contentType);
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }
}
