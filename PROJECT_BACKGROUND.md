# Project Background

This project was created as a first step toward a superhero-server speedster ability.

The long-term concept is a server/client Fabric mod where the server authorizes a movement ability and the client presents it as a speedster power. For now, this starter project ignores map integrations and complex superhero systems. It focuses on two core features:

```text
Find a route to a target location and move the player along that route at fixed 5x time-compressed speed.
```

## Design choice

This version does not use Baritone directly. It includes a simple server-side A* pathfinder so the source project can build without manually uploading a Baritone `.jar` or depending on a private local library.

The server calculates the path and then runs the 5x speedwalk movement itself. The client still receives a path packet so the mod can verify that the client is installed and hold forward/sprint animation state, but the server is authoritative for the actual path travel.

## Why both client and server are required

The server side is needed for:

- Command registration.
- Permission checks.
- Path calculation.
- Sending approved path data to the correct player.
- Running the authoritative 5x speedwalk movement task.
- Suppressing fall damage while speedwalk is active and briefly after it ends.
- Treating water surfaces as runnable speedwalk path surfaces.

The client side is needed for:

- Receiving the path packet.
- Confirming that the matching client mod is installed.
- Releasing local movement keys when server-side speedwalk starts or stops.
- Later: adding HUD, particles, interpolation, or sound effects.

## Why this is only a starter implementation

Minecraft terrain is complicated. A production-grade superhero travel system would need much more than this first version, including:

- Better terrain analysis.
- Chunk-aware long-distance routing.
- Moving target support.
- Replanning if the player gets stuck.
- Protection against dangerous blocks.
- Integration with claims/regions/protection mods.
- A real power/cooldown/energy system.
- Optional Baritone backend support.
- Optional custom map UI support.

## Current command set

```text
/speedwalk <x> <z>
/speedwalk block <x> <y> <z>
/speedwalk stop
```

The command requires operator permission by default.

## Future upgrade ideas

- Add a config file for maximum path distance and search limits.
- Add a permission node via LuckPerms-compatible integration.
- Add server-side cooldowns.
- Add a client HUD showing the remaining route.
- Add particles, sound effects, and better interpolation during 5x travel.
- Add an optional Baritone adapter for stronger pathfinding.
- Add destination selection from a custom map screen.
