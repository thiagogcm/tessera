package dev.nthings.tessera.showcase.e2e;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/// A scenario run against the published showcase in each listed engine, with a fresh [Session].
///
/// List one engine when the behavior depends on a feature only that engine implements, such as
/// cross-document view transitions or `::scroll-button()` in Chromium; the scenario is then not
/// generated for the other engine at all. `cases` names a static method of the test class that
/// returns the values of a parameterized scenario; each value runs in every listed engine and is
/// passed to the test method parameter of its type.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(BrowserSessions.class)
@interface BrowserTest {

  /// The engines that run this scenario.
  Engine[] value() default {Engine.CHROMIUM, Engine.FIREFOX};

  /// Name of a static method returning a `List` or `Stream` of cases, or empty for one run.
  String cases() default "";
}
