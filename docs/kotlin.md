# Kotlin

Tessera exposes a Java API that Kotlin can call directly. Follow [Getting started](getting-started.md#install-the-library-locally) to install the dependency. Use Kotlin 2.3.20 or newer for package-level JSpecify nullability in library dependencies; see [KT-83467](https://youtrack.jetbrains.com/issue/KT-83467).

## Compose a page

```kotlin
import dev.nthings.tessera.compiler.Compiler
import dev.nthings.tessera.compiler.PageDocument
import dev.nthings.tessera.core.Component
import dev.nthings.tessera.core.DocumentContext
import dev.nthings.tessera.core.Elements
import dev.nthings.tessera.core.Elements.*
import dev.nthings.tessera.core.IntoElement
import dev.nthings.tessera.core.Scope
import dev.nthings.tessera.css.ScopeStyle
import dev.nthings.tessera.css.Tokens

val notice = Component { _ -> div().child(p().text("Build completed")) }

data class MetricCard(val title: String, val value: String) : Scope {
    override fun configureScope(scope: ScopeStyle) {
        scope.root { it.p(Tokens.SPACE_5).rounded(Tokens.RADIUS_XL) }
    }

    override fun expand(context: DocumentContext): IntoElement =
        div().children(h2().text(title), p().text(value))
}

fun main() {
    val body = Elements.main().children(notice, MetricCard("Builds", "42"))
    println(Compiler.compile(PageDocument("Home", body)).html)
}
```

The example uses ordinary SAM callbacks with `it`. The receiver plugin described below is optional and changes callback syntax throughout the configured module.

## Map Java conventions to Kotlin

| Java API | Kotlin usage |
| --- | --- |
| Element factories | Import `Elements.*`; qualify `Elements.main()` because your `fun main()` takes precedence |
| Numeric factories taking `double` | Use `CssLength.rem(1.0)`, `CssDuration.ms(150.0)`, and `opacity(0.5)` |
| `CssLength.plus`, `minus`, `times` | Also available as `+`, `-`, and `*` |
| `Component` functional interface | `Component { context -> ... }` |
| `Scope` interface | Implement a class or data class with `expand`; override scope naming and styles as needed |
| Record accessors | Properties such as `diagnostic.rule`, `page.html`, and `site.pages` |
| Public sealed variants | Exhaustive `when` for `Element`, `TagName`, `Condition`, and `PageTree.Node` |
| Immutable `@CheckReturnValue` types | Enable `-Xreturn-value-checker=check` to warn about discarded derived values |

Tessera's member names avoid Kotlin keywords and standard scope-function names. For example, `div().apply { child(span()) }` uses Kotlin's `apply` and adds the span to the div. Component identity and lifetime rules remain the same as in the [component guide](consumer-guide.md).

## Enable receiver lambdas

`StyleBlock`, `ScopeBlock`, and `ElementBlock` carry `dev.nthings.tessera.foundation.KotlinReceiver`. Register that annotation with Kotlin's `sam-with-receiver` compiler plugin to make the configured object the callback receiver:

| Callback mode   | Example                                                  |
| --------------- | -------------------------------------------------------- |
| Ordinary SAM    | `div().on(State.HOVER) { it.bg(Tokens.SURFACE_SUBTLE) }` |
| Receiver plugin | `div().on(State.HOVER) { bg(Tokens.SURFACE_SUBTLE) }`    |

The plugin applies to all three callback types in the module. Enabled callbacks have a receiver and no `it` parameter. Use matching versions for the JVM and receiver plugins.

For Gradle Kotlin DSL:

```kotlin
plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.sam.with.receiver") version "2.4.21"
}

samWithReceiver {
    annotation("dev.nthings.tessera.foundation.KotlinReceiver")
}
```

For Maven, add the plugin and its dependency to your Kotlin build configuration, using the same `kotlin.version` property for both:

```xml
<plugin>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-maven-plugin</artifactId>
    <version>${kotlin.version}</version>
    <configuration>
        <compilerPlugins>
            <plugin>sam-with-receiver</plugin>
        </compilerPlugins>
        <pluginOptions>
            <option>sam-with-receiver:annotation=dev.nthings.tessera.foundation.KotlinReceiver</option>
        </pluginOptions>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-maven-sam-with-receiver</artifactId>
            <version>${kotlin.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

With receiver callbacks enabled, scope rules and conditions use nested blocks:

```kotlin
scope.root { p(Tokens.SPACE_5) }
    .select(":scope h2") { textColor(Tokens.TEXT_MUTED) }
    .on(Media.MOTION_SAFE) { root { transition(Transition.controlFeedback()) } }

val pulse = Keyframes.named("pulse")
    .start { opacity(1.0) }
    .at(50.0) { opacity(0.5) }
    .end { opacity(1.0) }
```

See [Styling](styling.md) for the underlying rules and values.
