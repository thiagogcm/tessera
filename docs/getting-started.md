# Getting started

Build a two-page site, preview it over HTTP, and use Tessera in your own Maven application. You need JDK 25. The repository includes a Maven wrapper; [Just](https://just.systems/) provides shortcuts for its commands.

## Run the example

From the repository root:

```sh
just guide
jwebserver -b 127.0.0.1 -d target/guide -p 8081
```

Open `http://localhost:8081/`. Follow the installation link, then open search and type `publishing`. Press Enter to open the matching section. The example includes a theme, a scoped component, an image asset, and search.

The complete source is [GuideSite.java](../tessera-showcase/src/main/java/dev/nthings/tessera/showcase/guide/GuideSite.java). It uses only public library APIs. To generate the same files without Just:

```sh
./mvnw compile exec:java \
  -Dexec.mainClass=dev.nthings.tessera.showcase.guide.GuideSite \
  -Dexec.args="$PWD/target/guide"
```

## Install the library locally

The current development version is `0.1.0-SNAPSHOT`. Install the library and its parent from this checkout before adding the dependency to a separate application:

```sh
./mvnw -pl tessera -am install
```

Add these settings to your application's POM:

```xml
<properties>
    <maven.compiler.release>25</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencies>
    <dependency>
        <groupId>dev.nthings</groupId>
        <artifactId>tessera</artifactId>
        <version>0.1.0-SNAPSHOT</version>
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

The compiler plugin version matches the repository build and supports the `release` setting.

## Build your first site

Save this class as `src/main/java/BuildSite.java` in your application:

```java
import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.compiler.SiteDefinition;
import dev.nthings.tessera.compiler.SiteRoute;
import dev.nthings.tessera.compiler.SiteWriter;
import dev.nthings.tessera.core.Elements;
import java.nio.file.Path;

public class BuildSite {
  public static void main(String[] args) {
    var site = SiteDefinition.of(
        new SiteRoute("/", new PageDocument("Home", Elements.main().children(
            Elements.h1().text("Home"),
            Elements.a("/guide/").text("Read the guide")))),
        new SiteRoute("/guide", new PageDocument("Guide", Elements.main().children(
            Elements.h1().text("Guide"),
            Elements.p().text("Upload the generated files to a static host.")))));

    SiteWriter.writeToDirectory(Compiler.compile(site), Path.of("target/site"));
  }
}
```

Run it from your application's root with Maven:

```sh
mvn compile org.codehaus.mojo:exec-maven-plugin:3.6.4:java -Dexec.mainClass=BuildSite
```

You can also run the class from your IDE. Compilation checks both pages and their local links before the writer creates the output directory.

## Preview and publish

Serve the generated directory:

```sh
jwebserver -b 127.0.0.1 -d target/site -p 8082
```

Open `http://localhost:8082/`, then follow the guide link. The output contains:

| File                       | Purpose                               |
| -------------------------- | ------------------------------------- |
| `index.html`               | Home page                             |
| `guide/index.html`         | Guide page                            |
| `assets/styles-<hash>.css` | Shared stylesheet                     |
| `search-index.json`        | Route and registered-heading metadata |

Upload the entire directory to your static host. These examples publish at the origin root. Configure directory indexes and a redirect from `/guide` to `/guide/` so relative links resolve consistently. See [Sites](sites.md#publish-and-preview) for output options and preview APIs.

Continue with the [component guide](consumer-guide.md) to build reusable UI, or [Styling](styling.md) to change its appearance. Use [Diagnostics](diagnostics.md) when compilation reports an error.
