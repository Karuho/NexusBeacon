## Compatibility

* Legacy distribution: Spigot 1.8.8–1.12.2 on Java 8.
* Modern distribution: Paper 1.21.1–26.2 on Java 21 or the newer JVM required
  by the corresponding Paper release.
* Minecraft/Paper 1.13.2–1.20.x is not supported by release 1.3.0.

For the next release, the required generation split is a planned
`NexusBeacon-Classic.jar` for Minecraft 1.13.2–1.20.4 and the existing
`NexusBeacon.jar` for Minecraft 1.20.5/1.20.6–26.2. The Classic artifact is not
part of the current release.

## Building

The Maven reactor contains platform API, Modern platform, Legacy platform, and
two distribution modules. Build and test the full reactor with:

```bash
mvn test
mvn package
```

The public artifact candidates are generated at:

```text
nexusbeacon-dist-modern/target/NexusBeacon.jar
nexusbeacon-dist-legacy/target/NexusBeacon-Legacy.jar
```

The Modern distribution targets Java 21 and retains current Paper behavior.
The Legacy distribution targets Java 8 and includes the validated supported
gameplay set: custom item identity, crafting, transactional placement/removal,
storage and restart persistence, effects, GUI, purchases/upgrades,
FurnaceBoost, base protection, trust, beam styles, and range visualization.

Legacy administrative hot reload is not supported, and no storage migration is
required for the unchanged release format. PlaceholderAPI remains an optional
Modern-only integration. A documented Legacy visual limitation remains: a
managed mixed-layer pyramid can show both native and custom vertical beams when
its first vanilla-compatible 3x3 layer still activates Minecraft's native beam.
This has no gameplay or storage impact.

Validate Modern sources against Paper 26.2 using JDK 25:

```bash
mvn -Ppaper-26.2 clean package
```

The plugin version in `plugin.yml` is populated from the Maven project version,
keeping the source metadata and generated JAR synchronized.

## Author

Created and maintained by Karuho.

Published under the DynaDev brand.

## License

NexusBeacon is source-available, but it is not open source.

You may view, study, and modify the code for private use only.

Without explicit written permission from the author, you may not:

* Sell, rent, sublicense, or redistribute this software.
* Distribute original or modified versions of this software.
* Publish forks, derivative works, or modified versions of this project.
* Claim this project, its source code, or any derivative work as your own.

See [LICENSE](LICENSE) for the complete license terms.
