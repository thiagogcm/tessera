# Tessera

Tessera compiles Java components into HTML, CSS, and static assets. It provides typed element and style builders, reusable UI components, and tools for compiling and publishing multi-page sites.

Use it to generate documentation, content sites, or HTML in a Java application. The compiler checks element references, local site links, and accessibility rules before returning output. Components use native browser controls and contribute JavaScript modules when they need filtering, tab selection, or tooltip support.

Tessera requires **JDK 25** and targets current stable **Chromium and Firefox**. See [browser support](docs/browser-support.md) for the feature contract and its limits. The opt-in [experimental browser backend](docs/browser-rendering.md) runs Java views through WebAssembly.

## Try it

From the repository root, with JDK 25 and [Just](https://just.systems/) installed:

```sh
just demo
```

Open `http://localhost:8080/` to explore the component showcase. The preview is a compiled snapshot; run the command again after changes.

For a smaller example, follow [Getting started](docs/getting-started.md). It covers local installation, a two-page site, and the files to publish.

## Compile a page

```java
import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.core.Elements;

void main() {
  var document = new PageDocument("Hello", Elements.main().children(
      Elements.h1().text("Hello"),
      Elements.p().text("Built with Tessera.")));

  System.out.println(Compiler.compile(document).html());
}
```

This Java 25 source file prints a complete HTML document with its stylesheet embedded. [Site compilation](docs/sites.md) shares stylesheets across routes and produces files for a static host.

## Documentation

| Task | Read |
| --- | --- |
| Install locally and build a first site | [Getting started](docs/getting-started.md) |
| Compose elements, components, identities, and interactions | [Component guide](docs/consumer-guide.md) |
| Set themes, layout, conditional styles, and animation | [Styling](docs/styling.md) |
| Define routes, assets, navigation, search, and hosting | [Sites](docs/sites.md) |
| Read compiler errors and trace elements to Java source | [Diagnostics](docs/diagnostics.md) |
| Call the Java API from Kotlin | [Kotlin](docs/kotlin.md) |
| Run Java views in the browser | [Browser rendering](docs/browser-rendering.md) |
| Check browser requirements and script behavior | [Browser support](docs/browser-support.md) |
| Build, test, and contribute | [Development](docs/development.md) |
| Understand package boundaries and rendering | [Architecture](docs/architecture.md), [compiler](docs/compiler.md), and [browser internals](docs/browser-internals.md) |
| Review the dual-backend design and delivery gates | [Proposal](docs/architecture-redesign.md), [authoring design](docs/design/authoring.md), [runtime design](docs/design/runtime.md), and [roadmap](docs/roadmap.md) |

## License

[Apache 2.0](LICENSE).
