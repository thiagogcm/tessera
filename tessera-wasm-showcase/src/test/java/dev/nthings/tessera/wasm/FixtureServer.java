package dev.nthings.tessera.wasm;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.nthings.tessera.compiler.CompiledSite;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/// Serves a compiled site as a deployment would: each page with its response headers, including the
/// full Content Security Policy, and each published file with its media type. It adds the endpoints
/// the demos request at runtime: `/text`, `/tone.wav`, and `/slow`, which answers only when the
/// server closes, so a request to it stays in flight until the page aborts it.
final class FixtureServer implements AutoCloseable {
  private static final byte[] TEXT = "hello from tessera".getBytes(StandardCharsets.UTF_8);

  private final CompiledSite site;
  private final HttpServer server;
  private final CountDownLatch closing = new CountDownLatch(1);

  FixtureServer(CompiledSite site) {
    this.site = site;
    try {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    server.createContext("/", this::handle);
    server.start();
  }

  /// The origin, without a trailing slash.
  String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  @Override
  public void close() {
    closing.countDown();
    server.stop(0);
  }

  private void handle(HttpExchange exchange) throws IOException {
    try (exchange) {
      var path = exchange.getRequestURI().getPath();
      switch (path) {
        case "/text" -> send(exchange, 200, "text/plain; charset=utf-8", TEXT);
        case "/tone.wav" -> send(exchange, 200, "audio/wav", silence());
        case "/slow" -> slow(exchange);
        default -> published(exchange, path);
      }
    }
  }

  private void published(HttpExchange exchange, String path) throws IOException {
    var file = site.assets().get(path);
    if (file != null) {
      send(exchange, 200, file.contentType(), file.content());
      return;
    }
    var route = path.endsWith("/index.html") ? path.substring(0, path.length() - 10) : path;
    if (route.length() > 1 && route.endsWith("/")) {
      route = route.substring(0, route.length() - 1);
    }
    var page = site.pages().get(route);
    if (page == null || !path.endsWith("/") && !path.endsWith(".html")) {
      send(
          exchange, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
      return;
    }
    site.headers().getOrDefault(route, Map.of()).forEach(exchange.getResponseHeaders()::set);
    send(exchange, 200, "text/html; charset=utf-8", page.getBytes(StandardCharsets.UTF_8));
  }

  private void slow(HttpExchange exchange) throws IOException {
    try {
      closing.await(60, TimeUnit.SECONDS);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
    send(exchange, 200, "text/plain; charset=utf-8", "late".getBytes(StandardCharsets.UTF_8));
  }

  private static void send(HttpExchange exchange, int status, String type, byte[] body)
      throws IOException {
    exchange.getResponseHeaders().set("Content-Type", type);
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    exchange.sendResponseHeaders(status, body.length);
    exchange.getResponseBody().write(body);
  }

  /// Two seconds of 8 kHz, 8-bit mono PCM silence, so playback advances in every engine.
  private static byte[] silence() {
    var samples = 16_000;
    var wav = ByteBuffer.allocate(44 + samples).order(ByteOrder.LITTLE_ENDIAN);
    wav.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + samples);
    wav.put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16);
    wav.putShort((short) 1).putShort((short) 1).putInt(8_000).putInt(8_000);
    wav.putShort((short) 1).putShort((short) 8);
    wav.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(samples);
    for (var index = 0; index < samples; index++) {
      wav.put((byte) 128);
    }
    return wav.array();
  }
}
