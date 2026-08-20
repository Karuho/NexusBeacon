package cl.dynasty.nexusbeacon.platform.legacy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Java 8 YAML backend compatible with the existing Modern beacon schema. */
public final class LegacyYamlBeaconStorage implements LegacyBeaconStorage {
    interface SnapshotEncoder { byte[] encode(Collection<LegacyBeaconState> beacons); }

    private final File file;
    private final File backupFile;
    private final SnapshotEncoder encoder;

    public LegacyYamlBeaconStorage(File file) {
        this(file, new DeterministicYamlEncoder());
    }

    LegacyYamlBeaconStorage(File file, SnapshotEncoder encoder) {
        if (file == null) throw new NullPointerException("file");
        if (encoder == null) throw new NullPointerException("encoder");
        this.file = file;
        this.backupFile = new File(file.getParentFile(), file.getName() + ".bak");
        this.encoder = encoder;
    }

    @Override public LegacyStorageLoadResult load() {
        if (!file.exists()) {
            if (backupFile.isFile()) {
                LegacyStorageLoadResult backup = parse(backupFile);
                if (!backup.isSuccessful()) {
                    return LegacyStorageLoadResult.failure(LegacyStorageLoadStatus.CORRUPT,
                            "Primary storage is missing and recovery backup is invalid");
                }
                try {
                    Files.copy(backupFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException exception) {
                    return LegacyStorageLoadResult.failure(LegacyStorageLoadStatus.UNAVAILABLE,
                            "Could not restore interrupted storage replacement");
                }
                return backup;
            }
            return LegacyStorageLoadResult.success(java.util.Collections.<LegacyBeaconState>emptyList());
        }
        if (!file.isFile() || !file.canRead()) {
            return LegacyStorageLoadResult.failure(LegacyStorageLoadStatus.UNAVAILABLE,
                    "Storage path is not a readable file");
        }
        return parse(file);
    }

    @Override public void store(Collection<LegacyBeaconState> beacons) {
        if (beacons == null) throw new NullPointerException("beacons");
        final byte[] bytes;
        try {
            bytes = encoder.encode(beacons);
        } catch (RuntimeException exception) {
            throw new LegacyStorageException("Could not serialize Legacy beacon state", exception);
        }

        File parentFile = file.getAbsoluteFile().getParentFile();
        Path temporary = null;
        try {
            Files.createDirectories(parentFile.toPath());
            temporary = Files.createTempFile(parentFile.toPath(), file.getName() + ".", ".tmp");
            writeAndSync(temporary.toFile(), bytes);
            LegacyStorageLoadResult verification = parse(temporary.toFile());
            if (!verification.isSuccessful() || verification.getBeacons().size() != beacons.size()
                    || !new HashSet<LegacyBeaconState>(verification.getBeacons())
                            .equals(new HashSet<LegacyBeaconState>(beacons))) {
                throw new LegacyStorageException("Serialized Legacy beacon snapshot failed verification");
            }

            if (file.isFile()) {
                Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
                syncExistingFile(backupFile);
            }
            replace(temporary, file.toPath());
            temporary = null;
        } catch (LegacyStorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new LegacyStorageException("Could not persist Legacy beacon state", exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    @Override public void close() { }
    @Override public String getBackendName() { return "YAML"; }

    private LegacyStorageLoadResult parse(File source) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(source);
            Object root = yaml.getValues(false).get("beacons");
            if (!(root instanceof ConfigurationSection)) {
                return LegacyStorageLoadResult.failure(LegacyStorageLoadStatus.CORRUPT,
                        "Required beacons section is missing");
            }
            ConfigurationSection section = (ConfigurationSection) root;
            TreeMap<String, LegacyBeaconState> byLocation = new TreeMap<String, LegacyBeaconState>();
            Set<UUID> uniqueIds = new HashSet<UUID>();
            for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
                if (!(entry.getValue() instanceof ConfigurationSection)) {
                    throw new IllegalArgumentException("Beacon entry is not a section");
                }
                LegacyBeaconState state = parseBeacon(entry.getKey(), (ConfigurationSection) entry.getValue());
                if (byLocation.put(state.getId(), state) != null || !uniqueIds.add(state.getUniqueId())) {
                    throw new IllegalArgumentException("Duplicate beacon location or unique id");
                }
            }
            return LegacyStorageLoadResult.success(new ArrayList<LegacyBeaconState>(byLocation.values()));
        } catch (IOException exception) {
            return LegacyStorageLoadResult.failure(LegacyStorageLoadStatus.UNAVAILABLE,
                    "Could not read Legacy storage");
        } catch (InvalidConfigurationException exception) {
            return LegacyStorageLoadResult.failure(LegacyStorageLoadStatus.CORRUPT,
                    "Legacy storage is not valid YAML");
        } catch (IllegalArgumentException exception) {
            return LegacyStorageLoadResult.failure(LegacyStorageLoadStatus.CORRUPT,
                    "Legacy storage contains invalid beacon data: " + exception.getMessage());
        }
    }

    private static LegacyBeaconState parseBeacon(String locationKey, ConfigurationSection section) {
        Map<String, Object> values = section.getValues(false);
        LegacyBeaconLocation location = LegacyBeaconLocation.parse(locationKey);
        UUID uniqueId = requiredUuid(values.get("unique-id"), "unique-id");
        UUID owner = optionalUuid(values.get("owner"), "owner");
        int range = requiredInteger(values.get("range"), "range");
        int level = requiredInteger(values.get("level"), "level");
        Map<String, Integer> effects = parseEffects(values.get("effects"));
        Set<String> active = parseStrings(values.get("active-effects"), "active-effects");
        Set<UUID> trusted = parseUuids(values.get("trusted"));
        boolean protect = requiredBoolean(values.get("protect-base-blocks"), "protect-base-blocks");
        boolean rangeParticles = requiredBoolean(values.get("range-particles-enabled"),
                "range-particles-enabled");
        String particleType = requiredString(values.get("range-particle-type"), "range-particle-type");
        String beamStyle = optionalString(values.get("beam-style"), "beam-style");
        return new LegacyBeaconState(location, uniqueId, owner, range, level, effects, active, trusted,
                protect, beamStyle, rangeParticles, particleType);
    }

    private static Map<String, Integer> parseEffects(Object raw) {
        if (!(raw instanceof ConfigurationSection)) throw new IllegalArgumentException("effects is not a section");
        TreeMap<String, Integer> effects = new TreeMap<String, Integer>();
        for (Map.Entry<String, Object> entry : ((ConfigurationSection) raw).getValues(false).entrySet()) {
            effects.put(entry.getKey(), Integer.valueOf(requiredInteger(entry.getValue(), "effect level")));
        }
        return effects;
    }

    private static Set<String> parseStrings(Object raw, String name) {
        if (!(raw instanceof List<?>)) throw new IllegalArgumentException(name + " is not a list");
        Set<String> values = new HashSet<String>();
        for (Object value : (List<?>) raw) values.add(requiredString(value, name));
        return values;
    }

    private static Set<UUID> parseUuids(Object raw) {
        Set<String> strings = parseStrings(raw, "trusted");
        Set<UUID> values = new HashSet<UUID>();
        for (String value : strings) values.add(parseUuid(value, "trusted"));
        return values;
    }

    private static UUID requiredUuid(Object raw, String name) {
        return parseUuid(requiredString(raw, name), name);
    }

    private static UUID optionalUuid(Object raw, String name) {
        String value = optionalString(raw, name);
        return value == null || value.isEmpty() ? null : parseUuid(value, name);
    }

    private static UUID parseUuid(String value, String name) {
        try {
            UUID parsed = UUID.fromString(value);
            if (!parsed.toString().equalsIgnoreCase(value)) throw new IllegalArgumentException();
            return parsed;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(name + " is not a canonical UUID");
        }
    }

    private static int requiredInteger(Object raw, String name) {
        if (!(raw instanceof Integer)) throw new IllegalArgumentException(name + " is not an integer");
        return ((Integer) raw).intValue();
    }

    private static boolean requiredBoolean(Object raw, String name) {
        if (!(raw instanceof Boolean)) throw new IllegalArgumentException(name + " is not a boolean");
        return ((Boolean) raw).booleanValue();
    }

    private static String requiredString(Object raw, String name) {
        if (!(raw instanceof String) || ((String) raw).isEmpty()) {
            throw new IllegalArgumentException(name + " is not a non-empty string");
        }
        return (String) raw;
    }

    private static String optionalString(Object raw, String name) {
        if (raw == null) return null;
        if (!(raw instanceof String)) throw new IllegalArgumentException(name + " is not a string");
        return (String) raw;
    }

    private static void replace(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException replacementFailure) {
                File backup = new File(target.toFile().getParentFile(), target.toFile().getName() + ".bak");
                if (!target.toFile().exists() && backup.isFile()) {
                    try { Files.copy(backup.toPath(), target, StandardCopyOption.REPLACE_EXISTING); }
                    catch (IOException restoreFailure) { replacementFailure.addSuppressed(restoreFailure); }
                }
                throw replacementFailure;
            }
        }
    }

    private static void writeAndSync(File target, byte[] bytes) throws IOException {
        FileOutputStream output = new FileOutputStream(target);
        try {
            output.write(bytes);
            output.flush();
            output.getFD().sync();
        } finally {
            output.close();
        }
    }

    private static void syncExistingFile(File target) throws IOException {
        FileOutputStream output = new FileOutputStream(target, true);
        try { output.getFD().sync(); } finally { output.close(); }
    }

    private static final class DeterministicYamlEncoder implements SnapshotEncoder {
        @Override public byte[] encode(Collection<LegacyBeaconState> beacons) {
            TreeMap<String, LegacyBeaconState> sorted = new TreeMap<String, LegacyBeaconState>();
            Set<UUID> uniqueIds = new HashSet<UUID>();
            for (LegacyBeaconState beacon : beacons) {
                if (beacon == null || sorted.put(beacon.getId(), beacon) != null
                        || !uniqueIds.add(beacon.getUniqueId())) {
                    throw new IllegalArgumentException("Duplicate or null beacon state");
                }
            }
            StringBuilder yaml = new StringBuilder();
            if (sorted.isEmpty()) {
                yaml.append("beacons: {}\n");
            } else {
                yaml.append("beacons:\n");
                for (LegacyBeaconState beacon : sorted.values()) appendBeacon(yaml, beacon);
            }
            return yaml.toString().getBytes(StandardCharsets.UTF_8);
        }

        private static void appendBeacon(StringBuilder yaml, LegacyBeaconState beacon) {
            yaml.append("  ").append(quote(beacon.getId())).append(":\n");
            line(yaml, "unique-id", beacon.getUniqueId().toString());
            line(yaml, "owner", beacon.getOwner() == null ? "" : beacon.getOwner().toString());
            integer(yaml, "range", beacon.getRange());
            integer(yaml, "level", beacon.getLevel());
            if (beacon.getEffectLevels().isEmpty()) {
                yaml.append("    effects: {}\n");
            } else {
                yaml.append("    effects:\n");
                for (Map.Entry<String, Integer> effect : beacon.getEffectLevels().entrySet()) {
                    yaml.append("      ").append(quote(effect.getKey())).append(": ")
                            .append(effect.getValue()).append('\n');
                }
            }
            stringList(yaml, "active-effects", beacon.getActiveEffects());
            stringList(yaml, "trusted", uuidStrings(beacon.getTrustedPlayers()));
            bool(yaml, "protect-base-blocks", beacon.isProtectBaseBlocks());
            bool(yaml, "range-particles-enabled", beacon.isRangeParticlesEnabled());
            line(yaml, "range-particle-type", beacon.getRangeParticleType());
            if (beacon.getBeamStyle() == null) yaml.append("    beam-style: null\n");
            else line(yaml, "beam-style", beacon.getBeamStyle());
        }

        private static List<String> uuidStrings(Set<UUID> values) {
            List<String> strings = new ArrayList<String>();
            for (UUID value : values) strings.add(value.toString());
            return strings;
        }

        private static void line(StringBuilder yaml, String key, String value) {
            yaml.append("    ").append(key).append(": ").append(quote(value)).append('\n');
        }

        private static void integer(StringBuilder yaml, String key, int value) {
            yaml.append("    ").append(key).append(": ").append(value).append('\n');
        }

        private static void bool(StringBuilder yaml, String key, boolean value) {
            yaml.append("    ").append(key).append(": ").append(value).append('\n');
        }

        private static void stringList(StringBuilder yaml, String key, Collection<String> values) {
            if (values.isEmpty()) {
                yaml.append("    ").append(key).append(": []\n");
                return;
            }
            yaml.append("    ").append(key).append(":\n");
            for (String value : values) yaml.append("    - ").append(quote(value)).append('\n');
        }

        private static String quote(String value) { return "'" + value.replace("'", "''") + "'"; }
    }
}
