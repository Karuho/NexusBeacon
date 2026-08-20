package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import cl.dynasty.nexusbeacon.platform.api.PlatformServices;

class LegacyNexusBeaconCommandTest {
    @Test void consoleHelpWorksAndPlayerOnlyReadFailsCleanly() {
        Fixture fixture = new Fixture();
        Sender console = Sender.console(false);

        fixture.command.onCommand(console.value, null, "nb", new String[0]);
        assertTrue(console.messages.toString().contains("NexusBeacon Legacy commands"));
        console.messages.clear();
        fixture.command.onCommand(console.value, null, "nb", new String[] { "trusted" });
        assertTrue(console.messages.toString().contains("Only players"));
    }

    @Test void permissionDenialDoesNotIssueAnItem() {
        Fixture fixture = new Fixture();
        Sender console = Sender.console(false);

        fixture.command.onCommand(console.value, null, "nb", new String[] { "give", "Someone" });

        assertTrue(console.messages.toString().contains("permission"));
        assertEquals(0, fixture.environment.onlineLookups);
    }

    @Test void trustMutationPersistsBeforePublishing() {
        Fixture fixture = new Fixture();
        Sender owner = Sender.player(fixture.owner, false, "Owner");
        Sender target = Sender.player(fixture.target, false, "Target");
        fixture.environment.target = fixture.state.find(fixture.location);
        fixture.environment.online = (Player) target.value;

        fixture.command.onCommand(owner.value, null, "nb", new String[] { "trust", "Target" });

        assertTrue(fixture.state.find(fixture.location).getTrustedPlayers().contains(fixture.target));
        assertTrue(fixture.storage.storeCalls > 0);
        assertTrue(owner.messages.toString().contains("now trusted"));
    }

    @Test void deferredBranchesAreAdminOnlyAndFailClosed() {
        Fixture fixture = new Fixture();
        Sender admin = Sender.console(true);

        fixture.command.onCommand(admin.value, null, "nb", new String[] { "storage", "migrate", "YAML", "MYSQL" });

        assertTrue(admin.messages.toString().contains("unavailable on Legacy"));
        assertEquals(1, fixture.state.size());
    }

    @Test void tabCompletionHidesAdministrativeBranches() {
        Fixture fixture = new Fixture();
        List<String> normal = fixture.command.onTabComplete(Sender.console(false).value, null, "nb",
                new String[] { "" });
        List<String> admin = fixture.command.onTabComplete(Sender.console(true).value, null, "nb",
                new String[] { "" });

        assertFalse(normal.contains("give"));
        assertTrue(admin.contains("give"));
        assertTrue(admin.contains("storage"));
    }

    private static final class Fixture {
        private final UUID owner = UUID.randomUUID();
        private final UUID target = UUID.randomUUID();
        private final LegacyBeaconLocation location = new LegacyBeaconLocation("world", 1, 64, 2);
        private final RecordingStorage storage = new RecordingStorage();
        private final LegacyApplicationState state = new LegacyApplicationState(storage);
        private final Environment environment = new Environment();
        private final LegacyNexusBeaconCommand command;

        private Fixture() {
            storage.beacons.add(new LegacyBeaconState(location, UUID.randomUUID(), owner, 48, 1,
                    Collections.<String, Integer>emptyMap(), Collections.<String>emptySet(),
                    Collections.<UUID>emptySet(), true, "classic", true, "VILLAGER_HAPPY"));
            state.initialize();
            LegacyMaterialResolver materials = new LegacyMaterialResolver();
            LegacyTextFormatter text = new LegacyTextFormatter();
            LegacyItemIdentityService identities = new LegacyItemIdentityService(new LegacyNbtBridge() {
                @Override public ItemStack mark(ItemStack item) { return item; }
                @Override public LegacyIdentityStatus identify(ItemStack item) {
                    return LegacyIdentityStatus.NOT_RECOGNIZED;
                }
                @Override public String getRevision() { return "test"; }
            });
            ImmediateScheduler scheduler = new ImmediateScheduler();
            LegacyParticleService particles = new LegacyParticleService(
                    LegacyParticleRuntime.SPIGOT_1_8, scheduler, new RecordingParticleTransport());
            LegacyApplicationGraph graph = new LegacyApplicationGraph(
                    new LegacyApplicationConfiguration("en_us", "YAML", "FIXED", 2, 1, 1, 0, 1, 0, 1, true),
                    new PlatformServices(scheduler, (entity, destination, cause) -> { }), identities, materials,
                    new LegacyPotionEffectResolver(), new LegacyGuiItemFactory(materials, text),
                    new LegacyInventoryFactory(), new LegacyMessageService(text), particles,
                    new LegacyBeamCompatibility(particles), state);
            LegacyBeaconItemFactory items = new LegacyBeaconItemFactory(identities, materials, text,
                    "&bNexusBeacon", Collections.singletonList("&7Marked"));
            command = new LegacyNexusBeaconCommand(graph, items, text, environment, "&b[NexusBeacon]&r ");
        }
    }

    private static final class Environment implements LegacyCommandEnvironment {
        private Player online;
        private LegacyBeaconState target;
        private int onlineLookups;
        @Override public Player findOnlinePlayer(String name) { onlineLookups++; return online; }
        @Override public LegacyBeaconState findTargetBeacon(Player player) { return target; }
    }

    private static final class RecordingStorage implements LegacyBeaconStorage {
        private final List<LegacyBeaconState> beacons = new ArrayList<LegacyBeaconState>();
        private int storeCalls;
        @Override public LegacyStorageLoadResult load() { return LegacyStorageLoadResult.success(beacons); }
        @Override public void store(Collection<LegacyBeaconState> values) {
            storeCalls++;
            beacons.clear();
            beacons.addAll(values);
        }
        @Override public void close() { }
        @Override public String getBackendName() { return "TEST"; }
    }

    private static final class Sender implements InvocationHandler {
        private final List<String> messages = new ArrayList<String>();
        private final boolean admin;
        private final UUID uuid;
        private final String name;
        private final CommandSender value;

        private Sender(Class<?> type, boolean admin, UUID uuid, String name) {
            this.admin = admin;
            this.uuid = uuid;
            this.name = name;
            value = (CommandSender) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, this);
        }
        static Sender console(boolean admin) { return new Sender(CommandSender.class, admin, null, "Console"); }
        static Sender player(UUID uuid, boolean admin, String name) { return new Sender(Player.class, admin, uuid, name); }
        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();
            if ("sendMessage".equals(methodName)) {
                if (args[0] instanceof String[]) Collections.addAll(messages, (String[]) args[0]);
                else messages.add(String.valueOf(args[0]));
                return null;
            }
            if ("hasPermission".equals(methodName)) return Boolean.valueOf(admin);
            if ("getUniqueId".equals(methodName)) return uuid;
            if ("getName".equals(methodName)) return name;
            if ("isOp".equals(methodName)) return Boolean.valueOf(admin);
            Class<?> result = method.getReturnType();
            if (result == Boolean.TYPE) return Boolean.FALSE;
            if (result == Integer.TYPE) return Integer.valueOf(0);
            if (result == Long.TYPE) return Long.valueOf(0L);
            if (result == Double.TYPE) return Double.valueOf(0D);
            if (result == Float.TYPE) return Float.valueOf(0F);
            if (result == Short.TYPE) return Short.valueOf((short) 0);
            if (result == Byte.TYPE) return Byte.valueOf((byte) 0);
            return null;
        }
    }
}
