package dev.nthings.tessera.showcase.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.opentest4j.AssertionFailedError;

/// Runs each [BrowserTest] once per engine and case, each run in a fresh [Session] on the
/// [PublishedSite].
///
/// One Playwright instance and one browser per engine serve the whole JVM. A run keeps its trace
/// and a full-page screenshot under `target/playwright/<engine>/<class>/` only when it fails, so
/// the artifacts on disk always belong to the latest failures.
final class BrowserSessions implements TestTemplateInvocationContextProvider {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(BrowserSessions.class);

  @Override
  public boolean supportsTestTemplate(ExtensionContext context) {
    return context.getTestMethod().map(m -> m.isAnnotationPresent(BrowserTest.class)).orElse(false);
  }

  @Override
  public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
      ExtensionContext context) {
    var method = context.getRequiredTestMethod();
    var test = method.getAnnotation(BrowserTest.class);
    var cases = cases(context.getRequiredTestClass(), test.cases());
    return Stream.of(test.value())
        .flatMap(engine ->
            IntStream.range(0, cases.size())
                .mapToObj(index -> new Run(engine, cases.get(index), index)));
  }

  private static List<@Nullable Object> cases(Class<?> testClass, String factory) {
    var cases = new ArrayList<@Nullable Object>();
    if (factory.isEmpty()) {
      cases.add(null);
      return cases;
    }
    try {
      Method source = testClass.getDeclaredMethod(factory);
      source.setAccessible(true);
      var values = source.invoke(null);
      if (values instanceof Stream<?> stream) {
        stream.forEach(cases::add);
      } else if (values instanceof Collection<?> collection) {
        cases.addAll(collection);
      } else {
        throw new IllegalStateException(factory + " must return a Stream or a Collection");
      }
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Cannot read cases from " + factory, e);
    }
    return cases;
  }

  /// One engine and case of a [BrowserTest].
  private record Run(Engine engine, @Nullable Object testCase, int index)
      implements TestTemplateInvocationContext {

    @Override
    public String getDisplayName(int invocationIndex) {
      return testCase == null ? "[" + engine + "]" : "[" + engine + "] " + testCase;
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
      return List.of(new Lifecycle(this));
    }
  }

  /// Opens the run's session before the test, and checks and closes it afterwards.
  private record Lifecycle(Run run)
      implements BeforeEachCallback, AfterTestExecutionCallback, ParameterResolver {

    @Override
    public void beforeEach(ExtensionContext context) {
      var browser = Browsers.shared(context).browser(run.engine());
      var site = PublishedSite.shared(context);
      context
          .getStore(NAMESPACE)
          .put(Session.class, new Session(run.engine(), browser, site.baseUrl()));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
      var type = parameter.getParameter().getType();
      return type == Session.class || (run.testCase() != null && type.isInstance(run.testCase()));
    }

    @Override
    public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
      if (parameter.getParameter().getType() == Session.class) {
        return session(context);
      }
      var value = run.testCase();
      if (value == null) {
        throw new IllegalStateException("No case for " + parameter.getParameter());
      }
      return value;
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
      var session = session(context);
      var failure = context.getExecutionException().orElse(null);
      var errors = new ArrayList<Throwable>();
      session.settle(errors);
      var problems = session.problems();
      session.finish(artifacts(context), failure != null || !problems.isEmpty(), errors);
      if (failure != null) {
        problems.forEach(problem -> failure.addSuppressed(new AssertionFailedError(problem)));
        errors.forEach(failure::addSuppressed);
      } else if (!problems.isEmpty()) {
        var error =
            new AssertionFailedError(
                "The page reported problems:\n  " + String.join("\n  ", problems));
        errors.forEach(error::addSuppressed);
        throw error;
      }
    }

    private static Session session(ExtensionContext context) {
      return context.getStore(NAMESPACE).get(Session.class, Session.class);
    }

    private Path artifacts(ExtensionContext context) {
      var name = context.getRequiredTestMethod().getName();
      if (run.testCase() != null) {
        name +=
            "-" + run.index() + "-" + run.testCase().toString().replaceAll("[^A-Za-z0-9-]+", "_");
      }
      return Path.of(
          "target",
          "playwright",
          run.engine().toString(),
          context.getRequiredTestClass().getSimpleName(),
          name);
    }
  }

  /// The JVM's Playwright driver and its browsers, launched on first use and closed at the end.
  private static final class Browsers implements AutoCloseable {
    private final Playwright playwright = Playwright.create();
    private final Map<Engine, Browser> browsers = new EnumMap<>(Engine.class);

    static Browsers shared(ExtensionContext context) {
      return context
          .getRoot()
          .getStore(NAMESPACE)
          .computeIfAbsent(Browsers.class, _ -> new Browsers(), Browsers.class);
    }

    Browser browser(Engine engine) {
      return browsers.computeIfAbsent(engine, e -> e.type(playwright).launch());
    }

    @Override
    public void close() {
      playwright.close();
    }
  }
}
