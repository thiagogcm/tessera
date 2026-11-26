package dev.nthings.tessera.compiler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Local preview server for a [CompiledSite], built on the JDK's `HttpServer` with virtual threads.
/// It listens on `127.0.0.1` only. Responses carry `Cache-Control: no-store`. A route requested
/// without its trailing slash gets a 308 redirect to its directory URL, keeping the query string,
/// and `index.html` aliases are accepted. Other paths return 404.
///
/// [#start(CompiledSite, int)] serves a fixed snapshot. [#watch(int, Supplier, Path...)] rebuilds
/// the site when files under the watched paths change and reloads open pages.
public final class SitePreviewServer implements AutoCloseable {

  static final String LIVE_PATH = "/_tessera/live.js";
  static final String VERSION_PATH = "/_tessera/version";
  private static final long QUIET_MILLIS = 100;
  private static final String LIVE_SCRIPT =
      """
      const version = new URL(import.meta.url).searchParams.get("v");
      setInterval(async () => {
        try {
          const response = await fetch("/_tessera/version", { cache: "no-store" });
          if ((await response.text()) !== version) location.reload();
        } catch {}
      }, 700);
      """;

  private record Snapshot(CompiledSite site, long version) {}

  private final HttpServer server;
  private final @Nullable WatchService watcher;

  private SitePreviewServer(HttpServer server, @Nullable WatchService watcher) {
    this.server = server;
    this.watcher = watcher;
  }

  /// Starts a server for `site` on `port`. Pass 0 to pick a free port, then read it with [#port()].
  /// Throws UncheckedIOException if the port cannot be bound.
  public static SitePreviewServer start(CompiledSite site, int port) {
    var snapshot = new AtomicReference<>(new Snapshot(site, 0));
    return new SitePreviewServer(listen(port, snapshot, false), null);
  }

  /// Builds the site with `build` and serves it on `port`, then calls `build` again whenever a file
  /// under one of the `sources` directories is created, changed or deleted. Changes arriving within
  /// 100 ms of each other trigger one rebuild. Pages get a small script that reloads them after a
  /// rebuild. A build that throws prints its stack trace to standard error and the last good site
  /// stays up. Java classes are not reloaded, so `build` sees changed content files but not changed
  /// code.
  ///
  /// Throws IllegalArgumentException if a source is not a directory, what the first `build` throws,
  /// and UncheckedIOException if the port cannot be bound or a source cannot be watched.
  public static SitePreviewServer watch(int port, Supplier<CompiledSite> build, Path... sources) {
    for (var source : sources) {
      if (!Files.isDirectory(source)) {
        throw new IllegalArgumentException("Not a directory: " + source);
      }
    }
    var snapshot = new AtomicReference<>(new Snapshot(build.get(), 1));
    WatchService watcher;
    try {
      watcher = FileSystems.getDefault().newWatchService();
      for (var source : sources) {
        register(watcher, source);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    var server = new SitePreviewServer(listen(port, snapshot, true), watcher);
    Thread.ofPlatform()
        .daemon()
        .name("tessera-preview-watch")
        .start(() -> rebuildOnChange(watcher, build, snapshot));
    return server;
  }

  private static HttpServer listen(int port, AtomicReference<Snapshot> snapshot, boolean live) {
    try {
      var httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
      httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
      httpServer.createContext("/", exchange -> handleRequest(snapshot.get(), live, exchange));
      httpServer.start();
      return httpServer;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void register(WatchService watcher, Path root) throws IOException {
    try (var dirs = Files.walk(root)) {
      for (var dir : (Iterable<Path>) dirs.filter(Files::isDirectory)::iterator) {
        dir.register(
            watcher,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE);
      }
    }
  }

  private static void rebuildOnChange(
      WatchService watcher, Supplier<CompiledSite> build, AtomicReference<Snapshot> snapshot) {
    try {
      while (true) {
        var key = watcher.take();
        do {
          var dir = (Path) key.watchable();
          for (var event : key.pollEvents()) {
            if (event.context() instanceof Path name && Files.isDirectory(dir.resolve(name))) {
              try {
                register(watcher, dir.resolve(name));
              } catch (IOException | UncheckedIOException e) {
                // Deleted again before it could be watched.
              }
            }
          }
          key.reset();
          key = watcher.poll(QUIET_MILLIS, TimeUnit.MILLISECONDS);
        } while (key != null);
        try {
          var site = build.get();
          snapshot.updateAndGet(current -> new Snapshot(site, current.version() + 1));
        } catch (RuntimeException e) {
          System.err.println("Tessera preview: rebuild failed, serving the last good site");
          e.printStackTrace();
        }
      }
    } catch (InterruptedException | ClosedWatchServiceException e) {
      // Closed by close().
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

  /// Stops the server immediately, without waiting for in-flight requests, and stops watching.
  @Override
  public void close() {
    server.stop(0);
    if (watcher != null) {
      try {
        watcher.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  private static void handleRequest(Snapshot snapshot, boolean live, HttpExchange exchange)
      throws IOException {
    var site = snapshot.site();
    var requestPath = exchange.getRequestURI().getPath();
    if (requestPath == null || requestPath.isBlank()) {
      requestPath = "/";
    }
    if (live && requestPath.equals(VERSION_PATH)) {
      sendResponse(exchange, 200, "text/plain; charset=utf-8", Long.toString(snapshot.version()));
      return;
    }
    if (live && requestPath.equals(LIVE_PATH)) {
      sendResponse(exchange, 200, "text/javascript; charset=utf-8", LIVE_SCRIPT);
      return;
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
    var normalized = SiteRoute.normalizePath(withoutIndex.isEmpty() ? "/" : withoutIndex);

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
      sendResponse(
          exchange,
          200,
          "text/html; charset=utf-8",
          live ? withLiveScript(page, snapshot.version()) : page);
      return;
    }

    sendResponse(exchange, 404, "text/plain; charset=utf-8", "404 Not Found: " + requestPath);
  }

  private static String withLiveScript(String page, long version) {
    var script = "<script type=\"module\" src=\"" + LIVE_PATH + "?v=" + version + "\"></script>";
    var end = page.lastIndexOf("</body>");
    return end < 0 ? page + script : page.substring(0, end) + script + page.substring(end);
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
