# NexusBeacon 1.3.0

NexusBeacon 1.3.0 provides two release artifacts for distinct server
generations:

- `NexusBeacon-Legacy.jar` supports Spigot 1.8.8–1.12.2 on Java 8.
- `NexusBeacon.jar` supports Paper 1.21.1–26.2 using Java 21 or the newer JVM
  required by the corresponding Paper release, including current Paper and
  Folia scheduling support.

Both generation lines provide the supported NexusBeacon application and
gameplay set: custom beacon identity, crafting, effects, GUI management, beam
styles, range visualization, FurnaceBoost, base protection, trusted-player
management, effect purchase and upgrade flows, and persistence across clean
server restarts. Modern identity uses PersistentDataContainer data; Legacy uses
its generation-compatible identity implementation.

## Compatibility notes

Minecraft/Paper 1.13.2–1.20.x is not supported by this release. A mandatory
Classic compatibility line for Minecraft 1.13.2–1.20.4 is planned for the next
release; no Classic artifact is included in 1.3.0. The next Modern line is
planned for Minecraft 1.20.5/1.20.6–26.2.

On Legacy servers, a managed mixed-layer NexusBeacon can visually show both
the native and custom vertical beams when its first vanilla-compatible 3×3
layer still activates Minecraft's native beam. This accepted visual limitation
has no gameplay or storage impact.
