package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LegacyGuiMutationServiceTest {
    @Test void ownerEffectTogglePersistsBeforePublication() {
        Fixture fixture = new Fixture();
        fixture.storage.observe = new Runnable() {
            @Override public void run() {
                assertFalse(fixture.state.find(fixture.location).getActiveEffects().contains("speed"));
            }
        };

        LegacyGuiMutationResult result = fixture.service.toggleEffect(fixture.session, fixture.owner, false,
                fixture.supportedEffect());

        assertEquals(LegacyGuiMutationResult.COMMITTED, result);
        assertTrue(fixture.state.find(fixture.location).getActiveEffects().contains("speed"));
        assertEquals(1, fixture.storage.storeCalls);
    }

    @Test void trustedPlayerCanMutateButUntrustedPlayerCannot() {
        Fixture fixture = new Fixture();
        assertEquals(LegacyGuiMutationResult.COMMITTED,
                fixture.service.selectBeamStyle(fixture.session, fixture.trusted, false, "red"));
        assertEquals("red", fixture.state.find(fixture.location).getBeamStyle());

        assertEquals(LegacyGuiMutationResult.UNAUTHORIZED,
                fixture.service.selectBeamStyle(fixture.session, UUID.randomUUID(), false, "green"));
        assertEquals("red", fixture.state.find(fixture.location).getBeamStyle());
    }

    @Test void unsupportedUnacquiredAndUnavailableRuntimeFailClosed() {
        Fixture fixture = new Fixture();
        assertEquals(LegacyGuiMutationResult.UNSUPPORTED,
                fixture.service.toggleEffect(fixture.session, fixture.owner, false, fixture.unsupportedEffect()));
        assertEquals(LegacyGuiMutationResult.NOT_ACQUIRED,
                fixture.service.toggleEffect(fixture.session, fixture.owner, false, fixture.otherEffect()));
        fixture.runtimeReady = false;
        assertEquals(LegacyGuiMutationResult.RUNTIME_UNAVAILABLE,
                fixture.service.toggleEffect(fixture.session, fixture.owner, false, fixture.supportedEffect()));
        assertEquals(0, fixture.storage.storeCalls);
    }

    @Test void removedBeaconAndReusedUuidAtAnotherLocationCannotBeRecreatedOrOverwritten() {
        Fixture fixture = new Fixture();
        fixture.state.delete(fixture.location);
        fixture.storage.storeCalls = 0;
        assertEquals(LegacyGuiMutationResult.MISSING_BEACON,
                fixture.service.selectBeamStyle(fixture.session, fixture.owner, false, "red"));
        assertEquals(0, fixture.state.size());

        LegacyBeaconState moved = fixture.beacon(new LegacyBeaconLocation("world", 9, 64, 9));
        fixture.state.insert(moved);
        fixture.storage.storeCalls = 0;
        assertEquals(LegacyGuiMutationResult.STALE_LOCATION,
                fixture.service.selectBeamStyle(fixture.session, fixture.owner, false, "green"));
        assertEquals("aqua", fixture.state.find(moved.getLocation()).getBeamStyle());
    }

    @Test void repeatedSelectionIsIdempotentAndDoesNotPersistAgain() {
        Fixture fixture = new Fixture();
        assertEquals(LegacyGuiMutationResult.COMMITTED,
                fixture.service.selectBeamStyle(fixture.session, fixture.owner, false, "aqua"));
        assertEquals(0, fixture.storage.storeCalls);
    }

    private static final class Fixture {
        private final UUID owner = UUID.randomUUID();
        private final UUID trusted = UUID.randomUUID();
        private final UUID beaconId = UUID.randomUUID();
        private final LegacyBeaconLocation location = new LegacyBeaconLocation("world", 1, 64, 2);
        private final Storage storage = new Storage();
        private final LegacyApplicationState state = new LegacyApplicationState(storage);
        private boolean runtimeReady = true;
        private final LegacyGuiSession session;
        private final LegacyGuiMutationService service;

        private Fixture() {
            storage.values.add(beacon(location));
            state.initialize();
            session = new LegacyGuiSessionRegistry().replace(owner, state.find(location), LegacyGuiMenu.MAIN);
            service = new LegacyGuiMutationService(state, new LegacyGuiMutationService.RuntimeReadiness() {
                @Override public boolean isReady() { return runtimeReady; }
            }, LegacyBeamStylePlan.currentDefaults());
        }

        private LegacyBeaconState beacon(LegacyBeaconLocation at) {
            Map<String, Integer> levels = new LinkedHashMap<String, Integer>();
            levels.put("speed", Integer.valueOf(1));
            Set<UUID> trustedPlayers = new LinkedHashSet<UUID>();
            trustedPlayers.add(trusted);
            return new LegacyBeaconState(at, beaconId, owner, 48, 1, levels,
                    Collections.<String>emptySet(), trustedPlayers, true, "aqua", true, "VILLAGER_HAPPY");
        }

        private LegacyEffectDefinition supportedEffect() {
            return new LegacyEffectDefinition("speed", "POTION", 3, "PLAYERS", null, 100, 1,
                    Collections.<org.bukkit.Material>emptyList(), true, "supported");
        }

        private LegacyEffectDefinition unsupportedEffect() {
            return new LegacyEffectDefinition("speed", "POTION", 3, "PLAYERS", null, 100, 1,
                    Collections.<org.bukkit.Material>emptyList(), false, "unavailable");
        }

        private LegacyEffectDefinition otherEffect() {
            return new LegacyEffectDefinition("haste", "POTION", 3, "PLAYERS", null, 100, 1,
                    Collections.<org.bukkit.Material>emptyList(), true, "supported");
        }
    }

    private static final class Storage implements LegacyBeaconStorage {
        private final List<LegacyBeaconState> values = new ArrayList<LegacyBeaconState>();
        private int storeCalls;
        private Runnable observe;
        @Override public LegacyStorageLoadResult load() { return LegacyStorageLoadResult.success(values); }
        @Override public void store(Collection<LegacyBeaconState> beacons) {
            if (observe != null) { observe.run(); observe = null; }
            storeCalls++;
            values.clear();
            values.addAll(beacons);
        }
        @Override public void close() { }
        @Override public String getBackendName() { return "TEST"; }
    }
}
