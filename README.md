# Speedster Pathwalk

Speedster Pathwalk is a Fabric mod source project for **Minecraft Java Edition 1.20.1**. It is a proof-of-concept for a superhero-server ability where an operator can run a command, have the server calculate a walkable route, and have the server move the player along that route at fixed **5x time-compressed speed**.

This is source code, not the final built mod file. Build it with GitHub Actions or Gradle, then install the built `.jar` from the build artifact.

## Target platform

- Minecraft: `1.20.1`
- Loader: Fabric Loader `0.16.14` or newer
- Java: 17
- Required runtime dependency: Fabric API `0.92.6+1.20.1` or another Fabric API build that supports Minecraft `1.20.1`
- Required sides: client and server

## Main features

- `/speedwalk <x> <z>` finds a surface target near the given X/Z coordinates.
- `/speedwalk block <x> <y> <z>` targets a specific block-position area.
- `/speedwalk stop` stops the current server-side speedwalk run.
- Server-side A* pathfinding over walkable block positions.
- If the exact target is not standable or cannot be reached, the pathfinder falls back to the nearest reachable point it can find instead of silently doing nothing.
- Fixed 5x time-compressed path travel. The mod does not use the vanilla Speed effect and does not change the player's movement-speed attribute.
- The server advances five normal-sized path movement slices per server tick, including vertical path changes, so upward/downward movement is compressed in time rather than letting normal-length jumps carry the player too far.
- Direct waypoint-to-waypoint yaw updates are kept intentionally, preserving the sharper/jankier speedster-autopilot feeling from the earlier direct-turning builds.
- GitHub Actions workflow using current action versions and uploading only the usable final `.jar` artifact.

## Basic usage after building

1. Install Fabric Loader for Minecraft `1.20.1` on the server and on each client.
2. Install Fabric API for Minecraft `1.20.1` on the server and on each client.
3. Put the built `speedster-pathwalk-0.2.3.jar` file into the `mods` folder on both the server and the client.
4. Start the server and client.
5. Make sure the player is an operator, or change the command permission rule in the source.
6. Run:

```text
/speedwalk 100 250
```

or:

```text
/speedwalk block 100 64 250
```

To stop the 5x path travel:

```text
/speedwalk stop
```

## Important limitations

This is intentionally a starter implementation. It does not break blocks, place blocks, swim intelligently, cross lava, fly, avoid all hazards, or understand every Minecraft terrain edge case. It searches within a safety limit and is intended for reachable, nearby terrain first. When the exact target cannot be used, it will choose a nearby standable target or the closest reachable point found during the search.

The 5x speed system is a first implementation of time-compressed path movement. The server moves the player along the already-calculated path in five normal-sized slices per tick and now syncs each slice instead of only syncing once per tick. This keeps the teleport-based model but makes the visible movement less chunky. The client also holds a forward/sprint state and directly nudges the vanilla limb animator during travel so the local player uses the sprint/running animation while the server controls the actual position. This is still not a full replacement for Minecraft's internal physics simulation. Some unusual terrain, modded blocks, moving blocks, deep water/underwater areas, vehicles, mounts, portals, elytra, or combat interactions may still need custom handling later.

The command is operator-only by default. For a real superhero server, replace the permission check with your power/ability system. The server checks whether the client has registered this mod before sending the path packet, so a missing client install should produce a clear command error instead of silent failure.

## Audit notes

See [`JUNE_2026_AUDIT.md`](JUNE_2026_AUDIT.md) for the June 2026 review notes and remaining validation limits.

## First file to read

Read [`START_HERE.md`](START_HERE.md) first. It explains how to upload the project to GitHub and build the final `.jar` without coding.
