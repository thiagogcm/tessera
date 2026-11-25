# Release guide

GitHub Actions builds Tessera on JDK 25. JReleaser signs the Maven artifacts, publishes them through the Central Portal, and creates the GitHub release. The workflow pushes the `v<version>` tag at the commit that produced the staged artifacts.

| Artifact                                       | Destination     |
| ---------------------------------------------- | --------------- |
| `dev.nthings:tessera`                          | Maven Central   |
| `dev.nthings:tessera-compiler`                 | Maven Central   |
| Release tag and Conventional Commits changelog | GitHub Releases |

Each Maven artifact includes a binary JAR, sources, Javadoc, a flattened POM, and a CycloneDX SBOM. The parent and showcases are reactor projects. The experimental Wasm backend is available from source via `-Pwasm`.

## Repository setup

Allow the workflow's `GITHUB_TOKEN` to push the release and next development commits to `main` and create release tags and releases. Confirm GitHub Actions can start jobs and choose the repository visibility before publishing. Maven Central artifacts, including the source JARs, are public.

Set these GitHub Actions repository secrets for both the dry run and the live release:

| Secret | Value |
| --- | --- |
| `JRELEASER_GPG_PUBLIC_KEY` | Armored public signing key |
| `JRELEASER_GPG_SECRET_KEY` | Armored private signing key |
| `JRELEASER_GPG_PASSPHRASE` | Signing key passphrase |
| `JRELEASER_MAVENCENTRAL_USERNAME` | Central Portal token username authorized for `dev.nthings` |
| `JRELEASER_MAVENCENTRAL_PASSWORD` | Central Portal token password |

Use the same valid publishing identity as adf4j if it still has access to `dev.nthings`. Make sure the signing key is available from a public keyserver.

## Cut 1.0.0

1. Confirm CI passes on `main` and review the consumer docs and public API for the release.
2. Open **Actions > Release > Run workflow** on `main`, enter `1.0.0` and `1.1.0-SNAPSHOT`, and leave **dryRun** enabled. From a checkout, the equivalent is:

   ```sh
   just release 1.0.0 1.1.0-SNAPSHOT
   just release-watch
   ```

3. Inspect the `release-diagnostics` artifact. The dry run sets the version in the runner, runs the JVM and browser checks, stages the Maven artifacts, and invokes `jreleaser full-release --dry-run`.
4. Dispatch again with **dryRun** disabled. The workflow commits the release version, publishes to Central, tags the exact release commit, creates the GitHub release, then commits the next snapshot version.

   ```sh
   just release 1.0.0 1.1.0-SNAPSHOT false
   just release-watch
   ```

The workflow defaults the next version to the next minor snapshot. Set it explicitly for release candidates, for example `1.0.0-rc.1` followed by `1.0.0-SNAPSHOT`.

For local staging, use JDK 25 and Playwright's browser dependencies:

```sh
just browsers --with-deps
just stage
find target/staging-deploy/dev/nthings -type f | sort
```

## Verify and recover

- Confirm `v1.0.0` points to the release-version commit, the GitHub release has the expected notes, and `main` is at `1.1.0-SNAPSHOT`.
- Resolve `dev.nthings:tessera-compiler:1.0.0` in a fresh Maven repository and confirm its transitive `dev.nthings:tessera:1.0.0` dependency resolves. Build the [consumer example](docs/getting-started.md) with the published artifacts.
- If a run fails, inspect the JReleaser diagnostics, the Central Portal deployment, and the GitHub tag and release before retrying. When no artifact has been published and no tag exists, correct the failure and dispatch again.
- If Central published successfully but GitHub release creation failed, ensure `v1.0.0` points to the exact release commit and run `jreleaser release --exclude-deployer-name=sonatype` from that commit with the release credentials and `JRELEASER_PROJECT_VERSION=1.0.0`. If only the final snapshot push failed, update the current `main` to the next development version with `just set-version` and push the version commit.
- Central releases are immutable. Publish a new patch version to correct an artifact that reached Central.
