package dev.nthings.tessera.wasm;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import dev.nthings.tessera.compiler.CompiledSite;
import dev.nthings.tessera.wasm.example.PublishedWeb;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

/// Shared browser fixture for every Wasm browser test in the failsafe JVM.
///
/// One [Fixture] per JVM compiles the showcase site from the images the build produced, serves it
/// with its deployment headers, and launches each engine once, on first use. Each test class gets
/// one browser context per engine, so pages reuse its caches, and each test gets a fresh page
/// through its [Session].
final class BrowserSuite implements TestTemplateInvocationContextProvider {
  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(BrowserSuite.class);

  /// Assertions wait for the Java entry to load, compile and mount.
  private static final double ASSERTION_TIMEOUT = 15_000;

  @Override
  public boolean supportsTestTemplate(ExtensionContext context) {
    return context.getRequiredTestMethod().isAnnotationPresent(BothEngines.class)
        || context.getRequiredTestMethod().isAnnotationPresent(Chromium.class);
  }

  @Override
  public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
      ExtensionContext context) {
    var engines =
        context.getRequiredTestMethod().isAnnotationPresent(BothEngines.class)
            ? List.of("chromium", "firefox")
            : List.of("chromium");
    return engines.stream().map(Invocation::new);
  }

  /// The fixture shared by every test class in this JVM.
  static Fixture fixture(ExtensionContext context) {
    return context
        .getRoot()
        .getStore(NAMESPACE)
        .computeIfAbsent(Fixture.class, _ -> new Fixture(), Fixture.class);
  }

  /// Compiled sites, their servers, Playwright and one browser per engine.
  static final class Fixture implements AutoCloseable {
    private final Path build = Path.of(System.getProperty("wasm.build"));
    private final Playwright playwright;
    private final Map<String, Browser> browsers = new HashMap<>();
    private final CompiledSite site;
    private final FixtureServer server;
    private @Nullable FixtureServer rootServer;

    Fixture() {
      PlaywrightAssertions.setDefaultAssertionTimeout(ASSERTION_TIMEOUT);
      site = compile("/assets/demo");
      server = new FixtureServer(site);
      playwright = Playwright.create();
    }

    private CompiledSite compile(String assetBase) {
      return PublishedWeb.compile(build, assetBase);
    }

    /// The showcase as [#server()] serves it, with its bundle files under `/assets/demo`.
    CompiledSite site() {
      return site;
    }

    /// The directory the build wrote the images and the published site to.
    Path build() {
      return build;
    }

    /// The fixture site, publishing its bundle files under `/assets/demo`.
    FixtureServer server() {
      return server;
    }

    /// The same site publishing its bundle files at the root asset base.
    FixtureServer rootServer() {
      var started = rootServer;
      if (started == null) {
        started = new FixtureServer(compile("/"));
        rootServer = started;
      }
      return started;
    }

    Playwright playwright() {
      return playwright;
    }

    Browser browser(String engine) {
      return browsers.computeIfAbsent(
          engine,
          name ->
              (name.equals("chromium") ? playwright.chromium() : playwright.firefox()).launch());
    }

    @Override
    public void close() {
      browsers.values().forEach(Browser::close);
      playwright.close();
      server.close();
      var started = rootServer;
      if (started != null) {
        started.close();
      }
    }
  }

  /// One browser context per engine for a test class, reporting policy violations as console errors
  /// so every session sees them.
  private record ClassContext(BrowserContext context) implements AutoCloseable {
    static ClassContext open(Browser browser) {
      var context = browser.newContext();
      context.addInitScript(
          """
          document.addEventListener('securitypolicyviolation', event =>
            console.error(`CSP violation: ${event.violatedDirective} ${event.blockedURI}`));
          """);
      return new ClassContext(context);
    }

    @Override
    public void close() {
      context.close();
    }
  }

  private record Invocation(String engine) implements TestTemplateInvocationContext {
    @Override
    public String getDisplayName(int invocationIndex) {
      return engine;
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
      return List.of(new SessionResolver(engine));
    }
  }

  /// Opens the test's session in its class's context, and checks and closes it afterwards.
  private record SessionResolver(String engine)
      implements ParameterResolver, AfterTestExecutionCallback {
    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
      return parameter.getParameter().getType() == Session.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
      var fixture = fixture(context);
      var owner = classContext(context);
      var browserContext =
          owner
              .getStore(NAMESPACE)
              .computeIfAbsent(
                  engine, _ -> ClassContext.open(fixture.browser(engine)), ClassContext.class)
              .context();
      var session = new Session(engine, browserContext.newPage(), fixture);
      context.getStore(NAMESPACE).put(Session.class, session);
      return session;
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
      var session = context.getStore(NAMESPACE).remove(Session.class, Session.class);
      if (session == null) {
        return;
      }
      try {
        var failure = context.getExecutionException();
        if (failure.isEmpty()) {
          session.verify();
        } else if (!session.errors().isEmpty()) {
          failure.get().addSuppressed(new AssertionError("Page errors: " + session.errors()));
        }
      } finally {
        session.close();
      }
    }

    private static ExtensionContext classContext(ExtensionContext context) {
      var current = context;
      while (current.getTestMethod().isPresent()) {
        current = current.getParent().orElseThrow();
      }
      return current;
    }
  }
}
