## Compatibility

* Java: 21+ for Minecraft 1.21.x; Java 25+ for Minecraft 26.2
* Server software: Paper/Purpur
* Supported Minecraft versions: 1.21.x and newer (validated through 26.2)
* Experimental: newer versions may work, but are not officially tested.

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
The Legacy distribution targets Java 8 and currently provides only the proven
platform detection and custom-NBT item-identity foundation; full Legacy
NexusBeacon gameplay is not implemented yet.

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
