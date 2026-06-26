# Changelog

## 0.2.3

- Added a client-side Mixin accessor for Minecraft's vanilla `LivingEntity` limb animator.
- While speedwalk is active, the client now forces the player's limb animator into a sprinting/running state each client tick.
- Kept the teleport-based 5x speedwalk movement model from 0.2.1.

## 0.2.1

- Kept the teleport-based 5x server runner.
- Changed speedwalk syncing from one visible teleport per server tick to one sync per 5x movement slice, making movement less chunky while preserving the fixed 5x rate.
- Added client-side animation assist so the local player holds forward/sprint animation state while the server controls path travel.
- Added final endpoint snapping to the resolved standable path target to reduce cases where the player ends one block too low.
- Tightened target completion so the pathfinder must actually reach the standable target block instead of accepting a nearby block one level too low.

## 0.2.0

- Added fixed 5x time-compressed speedwalk movement.
- Moved path execution to a server-side runner instead of relying on client movement keys.
- The server now advances five normal-sized path movement slices per server tick rather than applying a vanilla Speed effect or changing the movement-speed attribute.
- Vertical path changes are advanced inside the same slices, so jumps/steps/falls are compressed in time instead of producing long speed-effect-style jump carry.
- Kept the existing direct waypoint yaw behavior so the movement still has the sharper/jankier speedster-autopilot feel.
- The client now receives the path packet as a compatibility/status signal and releases movement keys instead of driving the movement itself.

## 0.1.5

- Reverted the 0.1.4 smooth-turning path follower.
- Restored the direct waypoint-to-waypoint yaw snapping behavior from 0.1.3.
- Kept the 0.1.3 fallback-path behavior for unreachable or non-standable exact targets.
- Updated documentation to clarify that sharp/janky turning is intentional in this version.

## 0.1.4

- Smoothed the client-side walking controller.
- Replaced instant yaw snapping with limited per-tick rotation.
- Added look-ahead steering so the player aims through the path instead of snapping from block center to block center.
- Added waypoint hysteresis so nearby path nodes are advanced more consistently.
- Reduced sprinting during sharp turns to avoid overshooting and camera jitter.

## 0.1.3

- Added nearest-reachable fallback behavior for unreachable or invalid targets.
- If an exact `/speedwalk block x y z` target is in the air, inside terrain, or otherwise not standable, the pathfinder now searches nearby columns for the nearest standable target.
- If the exact target cannot be reached by A*, the command now sends a path to the closest reachable point discovered during the search instead of doing nothing.
- Command feedback now explains whether the exact target was reached or a fallback destination was used.

## 0.1.2

Checked and updated against June 2026 tooling information.

Changed:

- Updated the GitHub Actions workflow to `actions/checkout@v6`, `actions/setup-java@v5`, `gradle/actions/setup-gradle@v6`, and `actions/upload-artifact@v6`.
- Configured the Gradle action to use the open-source `basic` cache provider.
- Updated Fabric Loom from `1.10.1` to `1.10.5`, which is available in the Fabric Maven repository.
- Tightened the declared Fabric Loader runtime requirement to `>=0.16.14`.
- Added a server-side networking capability check before sending path packets to the client.
- Adjusted the A* heuristic so downhill targets are less likely to be overestimated.
- Updated documentation and troubleshooting notes for the new version.

## 0.1.1

Fixed:

- Removed the Gradle `RepositoriesMode.FAIL_ON_PROJECT_REPOS` setting because Fabric Loom adds an internal `LoomLocalRemappedMods` repository during setup. That setting caused GitHub Actions builds to fail before compilation.
- Added troubleshooting notes for the `LoomLocalRemappedMods` / `prefer settings repositories` error.

## 0.1.0

Initial generated source project.

Added:

- Fabric 1.20.1 project structure.
- Server/client entrypoints.
- `/speedwalk <x> <z>` command.
- `/speedwalk block <x> <y> <z>` command.
- `/speedwalk stop` command.
- Server-side A* pathfinding prototype.
- Client-side walking follower prototype.
- GitHub Actions build workflow.
- Beginner-focused build and troubleshooting documentation.
