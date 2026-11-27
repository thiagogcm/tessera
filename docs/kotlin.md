# Kotlin

Call Tessera's Java API directly from Kotlin. Follow [Getting started](getting-started.md#install-the-library-locally) to install the local dependency. Use **Kotlin 2.3.20 or newer** so Kotlin reads Tessera's package-level JSpecify nullability annotations accurately (see [KT-83467](https://youtrack.jetbrains.com/issue/KT-83467)).

## Compose a page

```kotlin
import dev.nthings.tessera.compiler.Compiler
import dev.nthings.tessera.compiler.PageDocument
import dev.nthings.tessera.core.Component
import dev.nthings.tessera.core.Elements
import dev.nthings.tessera.core.Elements.*
import dev.nthings.tessera.core.Scope
import dev.nthings.tessera.core.TagElement
import dev.nthings.tessera.core.ViewContext
import dev.nthings.tessera.css.ScopeStyle
import dev.nthings.tessera.css.Tokens

val notice = Component { _ -> div().child(p().text("Build completed")) }

data class MetricCard(val title: String, val value: String) : Scope {
    override fun configureScope(scope: ScopeStyle) {
        scope.root { it.p(Tokens.SPACE_5).rounded(Tokens.RADIUS_XL) }
    }

    override fun expand(context: ViewContext): TagElement =
        div().children(h2().text(title), p().text(value))
}

fun main() {
    val body = Elements.main(notice, MetricCard("Builds", "42"))
    println(Compiler.forPage().compile(PageDocument.of("Home", body)).html)
}
```

This example uses standard SAM lambdas with `it`. Enabling the optional receiver plugin below replaces `it` with a Kotlin receiver across the configured module.

## Map Java conventions to Kotlin

| Java API | Kotlin usage |
| --- | --- |
| Element factories | Import `Elements.*`; qualify `Elements.main()` when a top-level `fun main()` is in scope |
| `double`-taking CSS factories | Pass doubles: `CssLength.rem(1.0)`, `CssDuration.ms(150.0)`, `opacity(0.5)` |
| `CssLength.plus`, `minus`, `times` | Available as `+`, `-`, and `*` operators |
| `Component` SAM interface | `Component { context -> ... }` |
| `Scope` interface | Implement a class or `data class` with `expand` and optional `scopeId`/`configureScope` |
| Java record accessors | Kotlin properties: `diagnostic.rule`, `page.html`, `site.pages` |
| Public sealed hierarchies | Exhaustive `when` over `Element`, `TagName`, `Condition`, `OutputMode`, `BrowserEvent`, `FetchResult`, and `PageTree.Node` |
| `@CheckReturnValue` value types | Pass `-Xreturn-value-checker=check` to flag discarded derived values |

Tessera member names avoid Kotlin keywords and standard scope-function names. For example, `div().apply { child(span()) }` invokes Kotlin's `apply` cleanly. Component identity, keying, and expansion rules match [Components](components.md).

## Enable receiver lambdas

`StyleBlock`, `ScopeBlock`, and `ElementBlock` carry `@dev.nthings.tessera.foundation.KotlinReceiver`. Register that annotation with Kotlin's `sam-with-receiver` compiler plugin to expose the builder as `this` instead of `it`:

| Callback style  | Example                                                  |
| --------------- | -------------------------------------------------------- |
| Standard SAM    | `div().on(State.HOVER) { it.bg(Tokens.SURFACE_SUBTLE) }` |
| Receiver plugin | `div().on(State.HOVER) { bg(Tokens.SURFACE_SUBTLE) }`    |

The plugin applies to all three callback types in the configured module; enabled callbacks no longer have an `it` parameter. Match the plugin version to your Kotlin compiler version.

### Gradle (Kotlin DSL)

```kotlin
plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.sam.with.receiver") version "2.3.20"
}

samWithReceiver {
    annotation("dev.nthings.tessera.foundation.KotlinReceiver")
}
```

### Maven

Use the same `${kotlin.version}` property for both `kotlin-maven-plugin` and `kotlin-maven-sam-with-receiver`:

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

With receiver lambdas enabled, nested style rules and keyframes read as DSL blocks:

```kotlin
scope.root { p(Tokens.SPACE_5) }
    .select(":scope h2") { textColor(Tokens.TEXT_MUTED) }
    .on(Media.MOTION_SAFE) { root { transition(Transition.controlFeedback()) } }

val pulse = Keyframes.named("pulse")
    .start { opacity(1.0) }
    .at(50.0) { opacity(0.5) }
    .end { opacity(1.0) }
```

See [Styling](styling.md) for the underlying CSS properties, layers, and token rules.
