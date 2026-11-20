# Tessera

Tessera compiles Java components into HTML, CSS, and static assets. It provides typed element and style builders, reusable UI components, and tools for compiling and publishing multi-page sites.

Use Tessera to build documentation, content sites, or server-rendered HTML in Java applications. The compiler checks element references, local links, form defaults, and accessibility rules before returning output. Built-in components rely on native browser controls and add small ES modules only when filtering, tab selection, menu focus return, or tooltips require them.

Tessera requires **JDK 25** and targets current stable **Chromium and Firefox**. See [Browser support](docs/browser-support.md) for platform features and progressive fallbacks. An opt-in [browser rendering backend](docs/browser-rendering.md) mounts the same portable components in the browser through WebAssembly.

## Try the showcase

From the repository root, with JDK 25 and [Just](https://just.systems/) installed:

```sh
just demo
```

Open `http://localhost:8080/` to browse the component showcase. The preview serves a compiled snapshot; run the command again after making edits.

For a minimal two-page application, follow [Getting started](docs/getting-started.md).

## Compile a page

```java
import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.core.Elements;

void main() {
  var document =
      new PageDocument(
          "Hello",
          Elements.main()
              .children(Elements.h1().text("Hello"), Elements.p().text("Built with Tessera.")));

  System.out.println(Compiler.compile(document).html());
}
```

This Java 25 program prints a complete HTML document with an embedded stylesheet. Use [Site compilation](docs/sites.md) to share one stylesheet across routes and produce files for a static host.

## Documentation

### Start and author

| Goal | Guide |
| --- | --- |
| Install locally, build a two-page site, and publish | [Getting started](docs/getting-started.md) |
| Build portable elements, components, scopes, forms, and identities | [Components](docs/components.md) |
| Build logical data tables with merges, sticky headers, and card mode | [Tables](docs/tables.md) |
| Compose static document features, outlines, footnotes, and page scripts | [Documents](docs/documents.md) |
| Configure themes, typed CSS, layout, container queries, and motion | [Styling](docs/styling.md) |
| Call the Java API from Kotlin and enable receiver lambdas | [Kotlin](docs/kotlin.md) |

### Publish and run in the browser

| Goal | Guide |
| --- | --- |
| Configure routes, output modes, assets, links, search, and previews | [Sites](docs/sites.md) |
| Read compiler diagnostics, check rule codes, and trace Java sources | [Diagnostics](docs/diagnostics.md) |
| Compile and mount live Java views through GraalVM Web Image | [Browser rendering](docs/browser-rendering.md) |
| Review supported engines, native platform features, and bundled scripts | [Browser support](docs/browser-support.md) |

### Architecture and contributing

| Goal | Guide |
| --- | --- |
| Understand modules, package boundaries, content families, and CSS layers | [Architecture](docs/architecture.md) |
| Follow static page preparation, site validation, and resource emission | [Compiler internals](docs/compiler.md) |
| Follow DOM reconciliation, commit ownership, lifecycles, and style ledgers | [Browser runtime internals](docs/browser-internals.md) |
| Run builds, quality gates, browser suites, and doc checks | [Development](docs/development.md) |
| Review remaining planned capabilities and release readiness criteria | [Roadmap](docs/roadmap.md) |

## License

[Apache 2.0](LICENSE).
