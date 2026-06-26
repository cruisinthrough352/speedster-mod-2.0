# Runtime and Build Troubleshooting

This file covers common mistakes when uploading, building, downloading, and installing the mod.

## Problem: I uploaded the original source zip to GitHub

GitHub Actions cannot build the project if the repository only contains the original zip file.

Fix:

1. Delete the uploaded zip from the repository.
2. Unzip the source zip on your computer.
3. Upload the extracted contents instead.
4. The repository root should show `build.gradle`, `src`, and `.github`.

## Problem: The project is inside an extra nested folder

Wrong layout:

```text
repository-name/speedster-pathwalk/README.md
repository-name/speedster-pathwalk/build.gradle
```

Correct layout:

```text
repository-name/README.md
repository-name/build.gradle
repository-name/src/
repository-name/.github/workflows/build.yml
```

Fix:

Move the inner folder contents up to the repository root.

## Problem: The hidden .github folder is missing

The build workflow is stored here:

```text
.github/workflows/build.yml
```

Some computers hide folders that begin with a dot. If `.github` was not uploaded, GitHub will not show the build workflow.

Fix:

1. Enable hidden files on your computer.
2. Re-upload the `.github` folder.
3. Confirm GitHub shows `.github/workflows/build.yml`.

## Problem: GitHub shows “Get started with GitHub Actions”

This usually means GitHub cannot find the workflow file.

Check that this exact file exists in the repository:

```text
.github/workflows/build.yml
```

Also check that it is not inside an extra folder.

## Problem: The workflow fails immediately with “build.gradle not found”

The workflow is running, but the project root is wrong.

Fix:

Move these files to the top level of the repository:

```text
build.gradle
settings.gradle
gradle.properties
src
```


## Problem: GitHub Actions fails with `LoomLocalRemappedMods` or `prefer settings repositories`

The error may look like this:

```text
Build was configured to prefer settings repositories over project repositories but repository 'LoomLocalRemappedMods' was added by plugin class 'net.fabricmc.loom.LoomRepositoryPlugin'
```

This was caused by a Gradle repository policy that rejects repositories added by plugins. Fabric Loom adds an internal repository while preparing the Minecraft development environment, so the project must not use `RepositoriesMode.FAIL_ON_PROJECT_REPOS`.

Fix:

1. Use this updated source zip, or open `settings.gradle`.
2. Make sure this line is not present:

```groovy
repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
```

3. Commit/upload the corrected `settings.gradle`.
4. Run the workflow again.

## Problem: I downloaded the workflow artifact but Minecraft does not load it

The GitHub artifact is a zip file. It is not the final mod file.

Fix:

1. Unzip the artifact.
2. Find the `.jar` file inside.
3. Put the `.jar` file into the Minecraft `mods` folder.

Do not install this file directly:

```text
speedster-pathwalk-built-mod-jar.zip
```

Install the `.jar` inside it.

## Problem: I installed the source zip instead of the built jar

The source zip contains `README.md`, `build.gradle`, and `src`. Minecraft cannot use that as a mod.

Fix:

Build the project first. Then install the final `.jar` from the build artifact.

## Problem: I am using the wrong Minecraft version

This project targets Minecraft `1.20.1`.

Fix:

Use a Fabric Loader profile for Minecraft `1.20.1` on the client and server. The mod declares Fabric Loader `0.16.14` or newer.

## Problem: Fabric API is missing

This project depends on Fabric API.

Fix:

Install Fabric API `0.92.6+1.20.1`, or another Fabric API `.jar` that supports Minecraft `1.20.1`, into the `mods` folder on both the server and the client.


## Problem: The command says the client has not registered the receiver

The error may look like this:

```text
That client has not registered the Speedster Pathwalk receiver. Install this mod on the client too, then reconnect.
```

This means the server has the mod, but the client either does not have the mod installed, has the wrong jar, or joined before the matching client mod was present.

Fix:

1. Put the same final built `.jar` file in the client `mods` folder.
2. Make sure the client is running Minecraft `1.20.1` with Fabric Loader.
3. Make sure Fabric API for `1.20.1` is installed on the client.
4. Restart the client and reconnect.

## Problem: The server has the mod but the client does not

This mod is required on both sides. The server calculates the path and controls the 5x movement. The client receives the path packet, confirms compatibility, and holds forward/sprint animation state while the server runner controls the actual position.

Fix:

Install the final built `.jar` on both the server and every client that will use `/speedwalk`.

## Problem: The command says I do not have permission

The command is operator-only by default.

Fix:

Use the server console to op yourself:

```text
op YourPlayerName
```

For a real superhero server, edit the command permission check in:

```text
src/main/java/com/example/speedsterpathwalk/command/SpeedwalkCommand.java
```

Replace the operator check with your server's ability/power check.

## Problem: The pathfinder cannot find a path

The starter pathfinder has safety limits. It does not break blocks, place blocks, bridge gaps, climb large cliffs, handle every fluid/lava case, or search infinitely far.

Try:

- Choosing a closer target.
- Choosing a flatter route.
- Using `/speedwalk block <x> <y> <z>` with a known walkable Y level.
- Testing first in a flat world.

## Problem: The player starts walking but gets stuck

The server runner follows the precomputed path and teleports the player after each of the five normal-sized movement slices per server tick. This is still teleport-based movement, but the visible gaps should be smaller than in 0.2.0. It may still behave poorly on unusual block shapes, doors, fences, trapdoors, ladders, vines, water, caves, moving blocks, vehicles, portals, or modded blocks.

Fix:

Use `/speedwalk stop`, move to a clearer starting position, and try a simpler target.

## Problem: I cannot find the final output file after building locally

Look here:

```text
build/libs/
```

Use the `.jar` that does not include `sources` or `dev` in its name.

## Problem: GitHub Actions ran on master but not main, or main but not master

The workflow is configured to run on both `main` and `master`, and it can also be run manually with **Run workflow**.

If the workflow does not appear, the issue is usually the missing or misplaced `.github/workflows/build.yml` file.


## Path goes to a nearby location instead of the exact target

Version `0.2.3` intentionally falls back when the exact target is not standable or cannot be reached. For example, if `/speedwalk block 10 200 10` points into the air, the mod searches nearby columns for a standable destination. If A* cannot reach the exact target, it sends the closest reachable path endpoint it found during the search. The command feedback should say when this happened.

If you expected the exact target to be reachable, check for cliffs, walls, missing stairs, closed doors, unloaded terrain, lava, or a path that requires breaking/placing blocks. This first version only walks; it does not mine, build bridges, fly, or parkour through complex terrain.

## The walking looks jittery or turns sharply

Version `0.2.3` keeps the direct waypoint-to-waypoint yaw behavior. The server rotates the player toward the active path segment during the 5x run instead of smoothing the camera rotation. This can look janky, especially on tight paths, but it is expected for this version.

If you want to reintroduce smoother turning later, edit `ServerSpeedwalkRunner.java` and add look-ahead steering plus capped yaw changes per tick. Similar client-side smoothing existed briefly in version `0.1.4`, but it was reverted because the sharper behavior was preferred for the current prototype.



## Problem: 5x movement feels like short teleports instead of perfect normal physics

Version `0.2.3` keeps the teleport-style speed implementation. It does not apply a vanilla Speed effect. It moves the player along the calculated path using five normal-sized path slices per server tick, then syncs each slice. This gives more visible position updates than 0.2.0, but it is still not a perfect recreation of Minecraft's entire internal movement simulation at 5x.

If this feels too choppy, the next likely improvement is to add client-side interpolation/particles while keeping the server authoritative. If jumps or stairs feel wrong, tune `NORMAL_STEP_BLOCKS`, `NODE_REACH_DISTANCE`, and the path simplification behavior in `ServerSpeedwalkRunner.java` and `ServerAStarPathfinder.java`.


## Problem: running animation still does not show correctly

Version `0.2.3` adds a stronger client-side animation assist. It holds forward/sprint input state and directly nudges the vanilla limb animator while speedwalk is active. This is intended to force the local sprint/running animation during teleport-based travel. If another client-side animation mod overrides player rendering or limb animation, that mod may still interfere.

## Problem: player still lands one block too low

Version `0.2.3` tightens exact target completion and snaps the final position to the resolved standable endpoint. If this still happens, check whether the destination is a half-block, stair, slab, carpet, modded collision shape, water edge, or a block whose collision shape is not a normal full cube. Those cases may need block-shape-specific handling later.


## Fall damage still happens during speedwalk

Version `0.2.3` cancels fall damage while a server-side speedwalk run is active and keeps a short grace period after speedwalk ends. Make sure the built `speedster-pathwalk-0.2.3.jar` is installed on the server, not only on the client. Fall-damage protection is server-side.

If fall damage still happens, check for another mod that replaces or heavily rewrites fall damage handling. Also check that the player was actually in an active `/speedwalk` run when the fall occurred; this feature is not a general permanent no-fall ability.

## Speedwalk does not run on water

Version `0.2.3` treats air directly above water as a runnable speedwalk surface. It does not make the player swim faster, walk underwater, or stand inside water blocks. If the path still avoids water, try choosing a target across shallow/open water with clear air above the water surface.

Waterlogged blocks, flowing-water edges, bubble columns, kelp, seagrass, modded fluids, boats, and underwater caves may still need custom handling later.
