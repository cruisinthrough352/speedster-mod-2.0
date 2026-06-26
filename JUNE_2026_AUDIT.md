# June 2026 Audit Notes

This file records the extra review done after the first GitHub Actions failure.

## What was checked

- The project zip layout was checked so the unzipped files appear at the project root, not inside an extra nested folder.
- The Gradle repository configuration was checked for the previous `LoomLocalRemappedMods` failure. `settings.gradle` no longer uses `RepositoriesMode.FAIL_ON_PROJECT_REPOS`.
- The project versions were checked against current public Fabric/GitHub Actions information available in June 2026.
- The GitHub Actions workflow was updated to current action major versions.
- The workflow still builds with Java 17 and Gradle 8.12.1 for Minecraft 1.20.1.
- The workflow collects only the usable built mod jar and excludes source/dev jars.
- The server command now checks whether the player client registered the mod networking receiver before sending a path.
- Version 0.2.3 keeps the server-side 5x speedwalk runner and changes it to sync each of the five movement slices per server tick rather than only the final tick position.
- The documentation was updated to match version `0.2.3`.
- Version 0.2.3 adds speedwalk-specific fall-damage protection and water-surface pathing. This was statically checked in the source project, but a full Gradle build still needs to run in GitHub Actions.

## Tooling choices

- Minecraft target: `1.20.1`.
- Java target: `17`.
- Fabric API dependency: `0.92.6+1.20.1`.
- Fabric Loader compile/runtime baseline: `0.16.14` or newer.
- Fabric Loom: `1.10.5`.
- GitHub Actions:
  - `actions/checkout@v6`
  - `actions/setup-java@v5`
  - `gradle/actions/setup-gradle@v6`
  - `actions/upload-artifact@v6`

## Important limitation of this audit

The ChatGPT sandbox used for this source package did not have Gradle installed and could not download Gradle from the internet. Because of that, a real local Gradle build could not be executed in the sandbox.

The project has been statically reviewed and patched for the known build problem, current action versions, dependency coordinates, and common beginner runtime issues. The intended real build target remains GitHub Actions, which has internet access and installs Gradle in the workflow.

## Expected final artifact

After a successful GitHub Actions build, the artifact named `speedster-pathwalk-built-mod-jar` should contain this usable mod file:

```text
speedster-pathwalk-0.2.3.jar
```

Install that `.jar` on both the Fabric 1.20.1 server and each Fabric 1.20.1 client that will use `/speedwalk`.
