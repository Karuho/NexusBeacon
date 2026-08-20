package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyYamlBeaconStorageTest {
    private static final UUID UNIQUE = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID OWNER = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID TRUSTED = UUID.fromString("33333333-3333-4333-8333-333333333333");

    @TempDir Path temporaryDirectory;

    @Test void distinguishesMissingAndEmptyStorageFromCorruptionAndUnavailability() throws Exception {
        File missing = temporaryDirectory.resolve("missing.yml").toFile();
        assertEquals(LegacyStorageLoadStatus.EMPTY, new LegacyYamlBeaconStorage(missing).load().getStatus());

        File empty = temporaryDirectory.resolve("empty.yml").toFile();
        Files.write(empty.toPath(), "beacons: {}\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(LegacyStorageLoadStatus.EMPTY, new LegacyYamlBeaconStorage(empty).load().getStatus());

        File corrupt = temporaryDirectory.resolve("corrupt.yml").toFile();
        Files.write(corrupt.toPath(), "beacons: [not-a-section\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(LegacyStorageLoadStatus.CORRUPT, new LegacyYamlBeaconStorage(corrupt).load().getStatus());

        File directory = temporaryDirectory.resolve("directory.yml").toFile();
        assertTrue(directory.mkdir());
        assertEquals(LegacyStorageLoadStatus.UNAVAILABLE,
                new LegacyYamlBeaconStorage(directory).load().getStatus());
    }

    @Test void roundTripsAllPersistentFieldsIncludingAnUnresolvedWorld() {
        File file = temporaryDirectory.resolve("storage.yml").toFile();
        LegacyYamlBeaconStorage storage = new LegacyYamlBeaconStorage(file);
        LegacyBeaconState expected = representative("unloaded_world", 8);
        storage.store(Collections.singleton(expected));
        LegacyStorageLoadResult loaded = storage.load();
        assertEquals(LegacyStorageLoadStatus.READY, loaded.getStatus());
        assertEquals(Collections.singletonList(expected), loaded.getBeacons());
    }

    @Test void producesDeterministicBytesRegardlessOfInputOrder() throws Exception {
        File file = temporaryDirectory.resolve("storage.yml").toFile();
        LegacyYamlBeaconStorage storage = new LegacyYamlBeaconStorage(file);
        LegacyBeaconState first = representative("z_world", 8);
        LegacyBeaconState second = new LegacyBeaconState(new LegacyBeaconLocation("a_world", -1, 70, 2),
                UUID.fromString("44444444-4444-4444-8444-444444444444"), null, 20, 2,
                Collections.<String, Integer>emptyMap(), Collections.<String>emptySet(),
                Collections.<UUID>emptySet(), false, null, false, "FLAME");
        storage.store(Arrays.asList(first, second));
        byte[] forward = Files.readAllBytes(file.toPath());
        storage.store(Arrays.asList(second, first));
        assertArrayEquals(forward, Files.readAllBytes(file.toPath()));
    }

    @Test void rejectsOneInvalidRecordWithoutReturningAPartialDataset() throws Exception {
        File file = temporaryDirectory.resolve("storage.yml").toFile();
        String yaml = modernFixture().replace("22222222-2222-4222-8222-222222222222", "not-a-uuid");
        Files.write(file.toPath(), yaml.getBytes(StandardCharsets.UTF_8));
        LegacyStorageLoadResult loaded = new LegacyYamlBeaconStorage(file).load();
        assertEquals(LegacyStorageLoadStatus.CORRUPT, loaded.getStatus());
        assertTrue(loaded.getBeacons().isEmpty());
        assertTrue(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).contains("not-a-uuid"));
    }

    @Test void serializationFailureNeverOverwritesTheKnownGoodFile() throws Exception {
        File file = temporaryDirectory.resolve("storage.yml").toFile();
        LegacyYamlBeaconStorage storage = new LegacyYamlBeaconStorage(file);
        storage.store(Collections.singleton(representative("world", 8)));
        byte[] knownGood = Files.readAllBytes(file.toPath());
        LegacyYamlBeaconStorage failing = new LegacyYamlBeaconStorage(file,
                new LegacyYamlBeaconStorage.SnapshotEncoder() {
                    @Override public byte[] encode(Collection<LegacyBeaconState> beacons) {
                        throw new IllegalStateException("synthetic serialization failure");
                    }
                });
        assertThrows(LegacyStorageException.class,
                () -> failing.store(Collections.singleton(representative("world", 8))));
        assertArrayEquals(knownGood, Files.readAllBytes(file.toPath()));
    }

    @Test void restoresAValidatedBackupAfterAnInterruptedFallbackLeavesNoPrimary() throws Exception {
        File file = temporaryDirectory.resolve("storage.yml").toFile();
        LegacyYamlBeaconStorage storage = new LegacyYamlBeaconStorage(file);
        LegacyBeaconState recoverable = representative("world", 8);
        storage.store(Collections.singleton(recoverable));
        LegacyBeaconState newer = new LegacyBeaconState(new LegacyBeaconLocation("other", 4, 70, 5),
                UUID.fromString("44444444-4444-4444-8444-444444444444"), null, 20, 1,
                Collections.<String, Integer>emptyMap(), Collections.<String>emptySet(),
                Collections.<UUID>emptySet(), false, null, false, "FLAME");
        storage.store(Collections.singleton(newer));
        Files.delete(file.toPath());
        LegacyStorageLoadResult recovered = storage.load();
        assertEquals(LegacyStorageLoadStatus.READY, recovered.getStatus());
        assertEquals(Collections.singletonList(recoverable), recovered.getBeacons());
        assertTrue(file.isFile());
    }

    @Test void readsARepresentativeModernYamlRecord() throws Exception {
        File file = temporaryDirectory.resolve("storage.yml").toFile();
        Files.write(file.toPath(), modernFixture().getBytes(StandardCharsets.UTF_8));
        LegacyStorageLoadResult loaded = new LegacyYamlBeaconStorage(file).load();
        assertEquals(LegacyStorageLoadStatus.READY, loaded.getStatus());
        assertEquals(representative("world", 8), loaded.getBeacons().get(0));
    }

    @Test void writesFieldsReadableThroughTheModernYamlAccessPattern() throws Exception {
        File file = temporaryDirectory.resolve("storage.yml").toFile();
        new LegacyYamlBeaconStorage(file).store(Collections.singleton(representative("world", 8)));
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(file);
        assertEquals(UNIQUE.toString(), yaml.getString("beacons.world;8;64;-3.unique-id"));
        assertEquals(2, yaml.getInt("beacons.world;8;64;-3.effects.speed"));
        assertTrue(yaml.getStringList("beacons.world;8;64;-3.active-effects").contains("speed"));
        assertFalse(yaml.getStringList("beacons.world;8;64;-3.trusted").isEmpty());
    }

    static LegacyBeaconState representative(String world, int x) {
        Map<String, Integer> effects = new LinkedHashMap<String, Integer>();
        effects.put("speed", Integer.valueOf(2));
        effects.put("haste", Integer.valueOf(1));
        Set<String> active = new LinkedHashSet<String>(Arrays.asList("speed"));
        Set<UUID> trusted = new LinkedHashSet<UUID>(Arrays.asList(TRUSTED));
        return new LegacyBeaconState(new LegacyBeaconLocation(world, x, 64, -3), UNIQUE, OWNER, 48, 1,
                effects, active, trusted, true, "aqua", true, "VILLAGER_HAPPY");
    }

    private static String modernFixture() {
        return "beacons:\n"
                + "  'world;8;64;-3':\n"
                + "    trusted:\n"
                + "    - '33333333-3333-4333-8333-333333333333'\n"
                + "    protect-base-blocks: true\n"
                + "    unique-id: '11111111-1111-4111-8111-111111111111'\n"
                + "    owner: '22222222-2222-4222-8222-222222222222'\n"
                + "    range: 48\n"
                + "    level: 1\n"
                + "    effects:\n"
                + "      haste: 1\n"
                + "      speed: 2\n"
                + "    active-effects:\n"
                + "    - speed\n"
                + "    range-particles-enabled: true\n"
                + "    range-particle-type: VILLAGER_HAPPY\n"
                + "    beam-style: aqua\n";
    }
}
