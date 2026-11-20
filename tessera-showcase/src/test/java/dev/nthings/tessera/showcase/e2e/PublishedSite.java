package dev.nthings.tessera.showcase.e2e;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;
import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.SiteWriter;
import dev.nthings.tessera.showcase.ShowcaseSite;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.extension.ExtensionContext;

/// The showcase and the [Fixtures] sites, compiled and written to disk once per JVM and served over
/// HTTP like any static host would serve them.
///
/// The showcase is served at `/`; each fixture site at `/fixtures/<name>/`.
final class PublishedSite implements AutoCloseable {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(PublishedSite.class);

  private final HttpServer server;
  private final String baseUrl;

  private PublishedSite(HttpServer server) {
    this.server = server;
    this.baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
  }

  /// The site shared by every test in this JVM, published on first use.
  static PublishedSite shared(ExtensionContext context) {
    return context
        .getRoot()
        .getStore(NAMESPACE)
        .computeIfAbsent(PublishedSite.class, _ -> publish(), PublishedSite.class);
  }

  /// The origin the site is served from, such as `http://127.0.0.1:41234`.
  String baseUrl() {
    return baseUrl;
  }

  private static PublishedSite publish() {
    try {
      var root = Files.createTempDirectory("tessera-showcase");
      SiteWriter.writeToDirectory(
          Compiler.forSite().compile(ShowcaseSite.createDefinition()), root);
      for (var fixture : Fixtures.all().entrySet()) {
        SiteWriter.writeToDirectory(
            fixture.getValue(), root.resolve(Path.of("fixtures", fixture.getKey())));
      }
      var server =
          SimpleFileServer.createFileServer(
              new InetSocketAddress("127.0.0.1", 0), root, SimpleFileServer.OutputLevel.NONE);
      // The JDK server answers every exchange on one dispatcher thread with blocking I/O, so a
      // browser that stops reading one response (a closed context, a cancelled prefetch) would
      // stall every later request. Serve each exchange on its own thread instead.
      server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
      server.start();
      return new PublishedSite(server);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot publish the showcase", e);
    }
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
