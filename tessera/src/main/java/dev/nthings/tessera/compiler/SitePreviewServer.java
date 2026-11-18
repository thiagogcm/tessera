package dev.nthings.tessera.compiler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;

/// Local preview server for a [CompiledSite], built on the JDK's `HttpServer` with virtual threads.
/// It listens on `127.0.0.1` only and serves a fixed snapshot of the site, so recompile and restart
/// after a change. Responses carry `Cache-Control: no-store`. A route requested without its
/// trailing slash gets a 308 redirect to its directory URL, keeping the query string, and
/// `index.html` aliases are accepted. Other paths return 404.
public final class SitePreviewServer implements AutoCloseable {

  private final HttpServer server;

  private SitePreviewServer(HttpServer server) {
    this.server = server;
  }

  /// Starts a server for `site` on `port`. Pass 0 to pick a free port, then read it with [#port()].
  /// Throws UncheckedIOException if the port cannot be bound.
  public static SitePreviewServer start(CompiledSite site, int port) {
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

  /// The port the server is listening on.
  public int port() {
    return server.getAddress().getPort();
  }

  /// The origin to open in a browser, `http://localhost:<port>`.
  public String baseUrl() {
    return "http://localhost:" + port();
  }

  /// Stops the server immediately, without waiting for in-flight requests.
  @Override
  public void close() {
    server.stop(0);
  }

  private static void handleRequest(CompiledSite site, HttpExchange exchange) throws IOException {
    var requestPath = exchange.getRequestURI().getPath();
    if (requestPath == null || requestPath.isBlank()) {
      requestPath = "/";
    }

    var file = site.files().get(requestPath);
    if (file != null) {
      sendBytes(exchange, 200, file.contentType(), file.contentView());
      return;
    }

    var withoutIndex =
        requestPath.endsWith("/index.html")
            ? requestPath.substring(0, requestPath.length() - "/index.html".length())
            : requestPath;
    var normalized = SiteRoute.normalizeRoutePath(withoutIndex.isEmpty() ? "/" : withoutIndex);

    var page = site.pages().get(normalized);
    if (page != null) {
      if (!requestPath.endsWith("/") && !requestPath.endsWith(".html")) {
        var query = exchange.getRequestURI().getRawQuery();
        exchange
            .getResponseHeaders()
            .set(
                "Location",
                SiteRoute.documentUriFor(normalized).toASCIIString()
                    + (query == null ? "" : "?" + query));
        exchange.sendResponseHeaders(308, -1);
        exchange.close();
        return;
      }
      var headers = exchange.getResponseHeaders();
      site.pageHeaders().getOrDefault(normalized, Map.of()).forEach(headers::set);
      sendResponse(exchange, 200, "text/html; charset=utf-8", page);
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
    try (var os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }
}
