# Build the Mod JAR

This project is source code. You must build it before using it as a Minecraft mod.

The final usable file is a `.jar` file created by the build. The source zip itself is not the final mod.

## Method 1: Build with GitHub Actions, no coding required

This is the recommended method for beginners.

1. Unzip the source project.
2. Create a new GitHub repository.
3. Upload the unzipped project contents to the repository root.
4. Make sure this file exists in GitHub:

```text
.github/workflows/build.yml
```

5. Click the **Actions** tab.
6. Click **Build Fabric Mod**.
7. Click **Run workflow**.
8. Open the completed workflow run.
9. Download the artifact named:

```text
speedster-pathwalk-built-mod-jar
```

10. Unzip the downloaded artifact.
11. Use the `.jar` file inside the artifact.

Expected final file name:

```text
speedster-pathwalk-0.2.3.jar
```

Do not install the artifact zip itself. Unzip it first.

## Method 2: Build locally with Gradle

Use this method only if you already have the required tools installed.

Required tools:

- Java Development Kit 17.
- Gradle 8.x. The GitHub workflow currently installs Gradle `8.12.1`, which is the version this project is pinned to for CI.
- Internet access so Gradle can download Fabric, Minecraft, mappings, and Fabric API dependencies.

Open a terminal in the project root. The project root is the folder containing:

```text
build.gradle
settings.gradle
gradle.properties
src
```

Run:

```bash
gradle build
```

When the build completes, find the final usable mod `.jar` here:

```text
build/libs/
```

Use the `.jar` that does **not** contain `sources` or `dev` in the name.

Correct example:

```text
build/libs/speedster-pathwalk-0.2.3.jar
```

Not the final mod file:

```text
build/libs/speedster-pathwalk-0.2.3-sources.jar
build/libs/speedster-pathwalk-0.2.3-dev.jar
```

## GitHub Actions versions

The workflow uses `actions/checkout@v6`, `actions/setup-java@v5`, `gradle/actions/setup-gradle@v6`, and `actions/upload-artifact@v6`. The Gradle action is configured with the open-source `basic` cache provider.

## Runtime installation

Install the final built `.jar` on both sides:

```text
server/mods/speedster-pathwalk-0.2.3.jar
client/mods/speedster-pathwalk-0.2.3.jar
```

Also install Fabric API on both sides.

## Changing the version or file name

The version and output name are controlled in `gradle.properties`:

```properties
mod_version=0.2.3
archives_base_name=speedster-pathwalk
```

After changing those values, rebuild the project.


## 0.2.3 runtime note

Fall damage protection and water running are included in the built `speedster-pathwalk-0.2.3.jar`. Install the same jar on both the Fabric server and every client using the command. The fall-damage cancellation is enforced server-side; the sprint animation assist is client-side.
