# Start Here: Build the Mod Without Coding

This guide assumes you do not know how to code and want GitHub to build the mod for you.

The file you downloaded from ChatGPT is a **source project zip**. It is not the finished mod `.jar`.

## 1. Download the zip

Download the zip file provided by ChatGPT.

Keep it somewhere easy to find, such as your Downloads folder or Desktop.

## 2. Unzip it

Right-click the downloaded zip file and choose one of these options:

- **Extract All...** on Windows.
- **Open With > Archive Utility** on macOS.
- **Extract Here** or **Extract To...** on Linux.

After unzipping, open the extracted folder.

You should immediately see files such as:

```text
README.md
START_HERE.md
BUILD_JAR.md
RUNTIME_TROUBLESHOOTING.md
build.gradle
settings.gradle
gradle.properties
src
.github
```

If you open the folder and only see one more folder inside it, open that inner folder. The files listed above must be the files you upload to GitHub.

## 3. Create a new GitHub repository

1. Go to GitHub in your web browser.
2. Sign in.
3. Click the **+** button in the top-right corner.
4. Click **New repository**.
5. Give it a name, such as `speedster-pathwalk`.
6. Choose **Public** or **Private**.
7. Do not worry about adding a README, .gitignore, or license on GitHub. This project already includes those files.
8. Click **Create repository**.

## 4. Upload the unzipped project contents

On the empty repository page:

1. Click **uploading an existing file**.
2. Open the unzipped project folder on your computer.
3. Select the project contents, not the original zip file.
4. Drag the selected files and folders into GitHub.
5. Make sure you upload the hidden `.github` folder too.
6. Scroll down.
7. Click **Commit changes**.

Important: do not upload the original source zip as the repository content. GitHub needs the unzipped files.

## 5. Check that the files are at the repository root

After the upload finishes, GitHub should show files like this at the top level of the repository:

```text
README.md
START_HERE.md
BUILD_JAR.md
RUNTIME_TROUBLESHOOTING.md
CHANGELOG.md
PROJECT_BACKGROUND.md
build.gradle
settings.gradle
gradle.properties
src
.github
```

Do not put everything inside an extra folder such as:

```text
speedster-pathwalk/speedster-pathwalk/README.md
```

The correct layout is:

```text
speedster-pathwalk/README.md
speedster-pathwalk/build.gradle
speedster-pathwalk/src/
speedster-pathwalk/.github/workflows/build.yml
```

## 6. Find the Actions tab

At the top of the GitHub repository page, click **Actions**.

If GitHub asks you to enable workflows, click the button to allow or enable them.

You should see a workflow named:

```text
Build Fabric Mod
```

If you only see **Get started with GitHub Actions**, read `RUNTIME_TROUBLESHOOTING.md`. The usual cause is that the `.github/workflows/build.yml` file was not uploaded to the repository root.

## 7. Run the build workflow

1. Click **Build Fabric Mod** in the left sidebar.
2. Click **Run workflow**.
3. Leave the branch set to `main` unless your repository uses `master`.
4. Click the green **Run workflow** button.
5. Wait for the workflow run to appear.
6. Click the workflow run to watch its progress.

The workflow can also run automatically when you push changes to `main` or `master`.

## 8. Download the workflow artifact

When the workflow finishes successfully:

1. Open the completed workflow run.
2. Scroll down to the **Artifacts** section.
3. Download the artifact named:

```text
speedster-pathwalk-built-mod-jar
```

This downloaded artifact is also a zip file.

## 9. Unzip the artifact

Right-click the downloaded artifact zip and extract it.

Do not put the artifact zip itself into Minecraft.

## 10. Find the final usable output file

Inside the extracted artifact, look for a file like:

```text
speedster-pathwalk-0.2.3.jar
```

That `.jar` file is the actual mod file.

Do not use these files as the final mod file:

```text
speedster-pathwalk-built-mod-jar.zip
source zip downloaded from ChatGPT
README.md
build.gradle
```

## 11. Install or use the final output file

Install the final `.jar` file like a normal Fabric mod:

1. Install Fabric Loader for Minecraft `1.20.1`.
2. Install Fabric API for Minecraft `1.20.1`.
3. Put the final `speedster-pathwalk-0.2.3.jar` file in the `mods` folder on the server.
4. Put the same `.jar` file in the `mods` folder on every client that will use the feature.
5. Start the server and client.
6. Join the server.
7. Use the command as an operator:

```text
/speedwalk 100 250
```

To target a specific Y level:

```text
/speedwalk block 100 64 250
```

To stop:

```text
/speedwalk stop
```


## Version 0.2.3 note

The 0.2.3 build includes fall-damage protection while speedwalking and lets speedwalk paths run across water surfaces. You still need to install the final built `.jar` on both the server and the client.
