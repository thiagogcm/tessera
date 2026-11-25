# Getting started

Build a two-page site, preview it over HTTP, and add Tessera to your own Maven application. You need **JDK 25**. The repository includes a Maven wrapper (`./mvnw`), and [Just](https://just.systems/) provides short commands for common tasks.

## Run the included example

From the repository root, build and serve the two-page guide example:

```sh
just guide
jwebserver -b 127.0.0.1 -d target/guide -p 8081
```

Open `http://localhost:8081/`. Follow the installation link, then open search, type `publishing`, and press Enter to jump to the matching section. The example uses a custom theme, a scoped component, an SVG asset, a table of contents, and client-side search.

The source lives in [GuideSite.java](../tessera-showcase/src/main/java/dev/nthings/tessera/showcase/guide/GuideSite.java) and calls only public library APIs. To run it without Just:

```sh
./mvnw compile exec:java \
  -Dexec.mainClass=dev.nthings.tessera.showcase.guide.GuideSite \
  -Dexec.args="$PWD/target/guide"
```

## Add Tessera to a Maven project

Tessera 1.0.0 provides a static compiler and a portable component library through Maven Central. Add the compiler dependency and Java settings to your application's `pom.xml`:

```xml
<properties>
    <maven.compiler.release>25</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencies>
    <dependency>
        <groupId>dev.nthings</groupId>
        <artifactId>tessera-compiler</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.16.0</version>
        </plugin>
    </plugins>
</build>
```

For a modular Java application, add `requires dev.nthings.tessera.compiler;` to `module-info.java`. That module exports the portable `dev.nthings.tessera` module transitively. Place your application classes in a named package and pass the fully qualified main class name when running Maven.

For portable component libraries that do not compile pages or sites, depend on `dev.nthings:tessera` instead.

## Build your first site

Save this class as `src/main/java/BuildSite.java`:

```java
import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.compiler.SiteDefinition;
import dev.nthings.tessera.compiler.SiteRoute;
import dev.nthings.tessera.compiler.SiteWriter;
import dev.nthings.tessera.core.Elements;
import java.nio.file.Path;

public class BuildSite {
  public static void main(String[] args) {
    var site =
        SiteDefinition.of(
            SiteRoute.of(
                "/",
                PageDocument.of("Home", Elements.main(h1("Home"), a("/guide/", "Read the guide")))),
            SiteRoute.of(
                "/guide",
                PageDocument.of(
                    "Guide",
                    Elements.main(h1("Guide"), p("Upload the generated files to a static host.")))));

    SiteWriter.writeToDirectory(Compiler.forSite().compile(site), Path.of("target/site"));
  }
}
```

Compile and run the generator from your application root:

```sh
mvn compile org.codehaus.mojo:exec-maven-plugin:3.7.0:java -Dexec.mainClass=BuildSite
```

You can also run `BuildSite` from your IDE. Compilation validates both pages and their local links before `SiteWriter` creates any output files.

## Preview and publish

Serve the generated directory over HTTP:

```sh
jwebserver -b 127.0.0.1 -d target/site -p 8082
```

Open `http://localhost:8082/` and follow the link to the guide page. The output directory contains:

| File                       | Purpose                             |
| -------------------------- | ----------------------------------- |
| `index.html`               | Home page                           |
| `guide/index.html`         | Guide page                          |
| `assets/styles-<hash>.css` | Shared, content-hashed stylesheet   |
| `search-index.json`        | Route and outlined heading metadata |

Upload the entire output directory to your static host. These examples assume hosting at the origin root; enable directory indexes and redirect `/guide` to `/guide/` so relative URLs resolve consistently. See [Sites](sites.md#publish-and-preview) for preview server APIs and hosting details.

## Next steps

- Compose reusable UI with [Components](components.md) and [Tables](tables.md).
- Add static outlines, footnotes, and scripts with [Documents](documents.md).
- Customize design tokens, layouts, and motion with [Styling](styling.md).
- Inspect compiler errors and trace elements with [Diagnostics](diagnostics.md).
