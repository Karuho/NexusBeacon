package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.lang.reflect.Proxy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

class LegacyRecipeManagerTest {
    @BeforeAll static void installMinimalBukkitItemFactory() {
        if (Bukkit.getServer() != null) return;
        final ItemFactory itemFactory = (ItemFactory) Proxy.newProxyInstance(
                ItemFactory.class.getClassLoader(), new Class<?>[] { ItemFactory.class },
                (instance, method, args) -> method.getName().equals("equals")
                        ? Boolean.valueOf(args[0] == args[1]) : defaultValue(method.getReturnType()));
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(),
                new Class<?>[] { Server.class }, (instance, method, args) -> {
                    if (method.getName().equals("getItemFactory")) return itemFactory;
                    if (method.getName().equals("getLogger")) return Logger.getLogger("LegacyRecipeManagerTest");
                    if (method.getName().equals("getName")) return "TestServer";
                    if (method.getName().equals("getVersion")) return "1.8.8-test";
                    if (method.getName().equals("getBukkitVersion")) return "1.8.8-R0.1-test";
                    return defaultValue(method.getReturnType());
                });
        Bukkit.setServer(server);
    }
    @Test void registersOnceAndSecondAttemptDoesNotDuplicate() {
        Fixture fixture = new Fixture();
        assertEquals(LegacyRecipeRegistrationStatus.REGISTERED,
                fixture.manager.register(recipe()).getStatus());
        assertEquals(LegacyRecipeRegistrationStatus.ALREADY_PRESENT,
                fixture.manager.register(recipe()).getStatus());
        assertEquals(1, fixture.backend.recipes.size());
        assertEquals(1, fixture.backend.addCalls);
    }

    @Test void newManagerDetectsRecipeLeftByReloadLikeInitialization() {
        Fixture first = new Fixture();
        assertTrue(first.manager.register(recipe()).isAvailable());
        LegacyRecipeManager second = first.newManager();

        assertEquals(LegacyRecipeRegistrationStatus.ALREADY_PRESENT,
                second.register(recipe()).getStatus());
        assertEquals(1, first.backend.recipes.size());
    }

    @Test void canonicalizedCraftBukkitSymbolsStillMatchSemanticGrid() {
        Fixture fixture = new Fixture();
        ShapedRecipe canonical = new ShapedRecipe(fixture.bridge.mark(new ItemStack(Material.BEACON)));
        canonical.shape("abc", "def", "ghi");
        Material[] grid = { Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK,
                Material.DIAMOND_BLOCK, Material.BEACON, Material.DIAMOND_BLOCK,
                Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN };
        for (int index = 0; index < grid.length; index++) {
            canonical.setIngredient((char) ('a' + index), grid[index]);
        }
        fixture.backend.recipes.add(canonical);

        assertEquals(LegacyRecipeRegistrationStatus.ALREADY_PRESENT,
                fixture.manager.register(recipe()).getStatus());
        assertEquals(1, fixture.manager.unregister());
        assertTrue(fixture.backend.recipes.isEmpty());
    }

    @Test void disableRemovesOnlyExactMarkedRecipeAndReinitDoesNotAccumulate() {
        Fixture fixture = new Fixture();
        fixture.manager.register(recipe());
        fixture.backend.recipes.add(unrelatedRecipe());

        assertEquals(1, fixture.manager.unregister());
        assertEquals(1, fixture.backend.recipes.size());
        assertEquals(Material.STONE, fixture.backend.recipes.get(0).getResult().getType());
        assertEquals(LegacyRecipeRegistrationStatus.REGISTERED,
                fixture.manager.register(recipe()).getStatus());
        assertEquals(2, fixture.backend.recipes.size());
    }

    @Test void unmodifiableRegistryIsLeftIntactAndReinitStillDoesNotDuplicate() {
        Fixture fixture = new Fixture();
        fixture.manager.register(recipe());
        fixture.backend.removalSupported = false;

        assertEquals(0, fixture.manager.unregister());
        assertEquals(1, fixture.backend.recipes.size());
        assertEquals(LegacyRecipeRegistrationStatus.ALREADY_PRESENT,
                fixture.newManager().register(recipe()).getStatus());
        assertEquals(1, fixture.backend.recipes.size());
    }

    @Test void invalidUnsupportedDisabledAndBukkitRejectionAreExplicitAndDoNotAdd() {
        Fixture fixture = new Fixture();
        YamlConfiguration invalid = recipe();
        invalid.set("shape", Arrays.asList("DDD"));
        assertEquals(LegacyRecipeRegistrationStatus.INVALID_CONFIG,
                fixture.manager.register(invalid).getStatus());

        YamlConfiguration unsupported = recipe();
        unsupported.set("ingredients.D", "NETHERITE_BLOCK");
        assertEquals(LegacyRecipeRegistrationStatus.UNSUPPORTED_INGREDIENT,
                fixture.manager.register(unsupported).getStatus());

        YamlConfiguration disabled = recipe();
        disabled.set("enabled", Boolean.FALSE);
        assertEquals(LegacyRecipeRegistrationStatus.DISABLED,
                fixture.manager.register(disabled).getStatus());

        fixture.backend.accept = false;
        assertEquals(LegacyRecipeRegistrationStatus.BUKKIT_REJECTED,
                fixture.manager.register(recipe()).getStatus());
        assertEquals(0, fixture.backend.recipes.size());
    }

    @Test void recipeResultCloneRetainsIdentityAndPlacementUsesSameIdentityPath() {
        Fixture fixture = new Fixture();
        fixture.manager.register(recipe());
        ItemStack result = fixture.backend.recipes.get(0).getResult();
        ItemStack cloned = result.clone();

        assertTrue(fixture.identities.identify(result).isRecognized());
        assertTrue(fixture.identities.identify(cloned).isRecognized());
        assertTrue(result.isSimilar(cloned));
        assertFalse(result.isSimilar(new ItemStack(Material.BEACON)));

        RecordingStorage storage = new RecordingStorage();
        LegacyApplicationState state = new LegacyApplicationState(storage);
        state.initialize();
        LegacyBeaconTransactionService transactions = new LegacyBeaconTransactionService(state,
                fixture.identities, new LegacyBeaconGameplaySettings(48, true, true,
                        "VILLAGER_HAPPY", true, true, true, true));
        LegacyBeaconTransactionResult placement = transactions.place(result,
                new LegacyBeaconLocation("world", 4, 64, 4), UUID.randomUUID());
        assertTrue(placement.isCommitted());
        assertEquals(1, storage.lastStored.size());
    }

    @Test void recipeCreatedAndPortableReturnedItemsRemainMarkedButDoNotMerge() {
        Fixture fixture = new Fixture();
        fixture.manager.register(recipe());
        ItemStack crafted = fixture.backend.recipes.get(0).getResult();
        ItemStack returned = new ItemStack(Material.BEACON, 1, (short) 8);

        assertTrue(fixture.identities.identify(crafted).isRecognized());
        assertTrue(fixture.identities.identify(returned).isRecognized());
        assertFalse(crafted.isSimilar(returned));
        assertFalse(crafted.isSimilar(new ItemStack(Material.BEACON)));
    }

    private static YamlConfiguration recipe() {
        YamlConfiguration recipe = new YamlConfiguration();
        recipe.set("enabled", Boolean.TRUE);
        recipe.set("shape", Arrays.asList("DDD", "DBD", "OOO"));
        recipe.set("ingredients.B", "BEACON");
        recipe.set("ingredients.D", "DIAMOND_BLOCK");
        recipe.set("ingredients.O", "OBSIDIAN");
        return recipe;
    }

    private static ShapedRecipe unrelatedRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(new ItemStack(Material.STONE));
        recipe.shape("S");
        recipe.setIngredient('S', Material.STONE);
        return recipe;
    }

    private static final class Fixture {
        private final DurabilityBridge bridge = new DurabilityBridge();
        private final LegacyItemIdentityService identities = new LegacyItemIdentityService(bridge);
        private final RecordingBackend backend = new RecordingBackend();
        private final LegacyRecipeManager manager = newManager();

        private LegacyRecipeManager newManager() {
            return new LegacyRecipeManager(new LegacyMaterialResolver(), amount -> {
                ItemStack item = new ItemStack(Material.BEACON, amount);
                return bridge.mark(item);
            }, identities, backend);
        }
    }

    private static final class DurabilityBridge implements LegacyNbtBridge {
        @Override public ItemStack mark(ItemStack item) { item.setDurability((short) 7); return item; }
        @Override public LegacyIdentityStatus identify(ItemStack item) {
            return item != null && (item.getDurability() == 7 || item.getDurability() == 8)
                    ? LegacyIdentityStatus.RECOGNIZED : LegacyIdentityStatus.NOT_RECOGNIZED;
        }
        @Override public ItemStack writePortableData(ItemStack item, LegacyPortableBeaconData data) {
            item.setDurability((short) 8); return item;
        }
        @Override public Optional<LegacyPortableBeaconData> readPortableData(ItemStack item) {
            return Optional.empty();
        }
        @Override public String getRevision() { return "test"; }
    }

    private static final class RecordingBackend implements LegacyRecipeBackend {
        private final List<Recipe> recipes = new ArrayList<Recipe>();
        private boolean accept = true;
        private boolean removalSupported = true;
        private int addCalls;
        @Override public Iterator<Recipe> recipes() {
            final Iterator<Recipe> delegate = recipes.iterator();
            return new Iterator<Recipe>() {
                @Override public boolean hasNext() { return delegate.hasNext(); }
                @Override public Recipe next() { return delegate.next(); }
                @Override public void remove() {
                    if (!removalSupported) throw new UnsupportedOperationException();
                    delegate.remove();
                }
            };
        }
        @Override public boolean add(Recipe recipe) {
            addCalls++;
            if (accept) recipes.add(recipe);
            return accept;
        }
    }

    private static final class RecordingStorage implements LegacyBeaconStorage {
        private List<LegacyBeaconState> lastStored = Collections.emptyList();
        @Override public LegacyStorageLoadResult load() {
            return LegacyStorageLoadResult.success(Collections.<LegacyBeaconState>emptyList());
        }
        @Override public void store(Collection<LegacyBeaconState> beacons) {
            lastStored = new ArrayList<LegacyBeaconState>(beacons);
        }
        @Override public void close() { }
        @Override public String getBackendName() { return "TEST"; }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return Boolean.FALSE;
        if (type == byte.class) return Byte.valueOf((byte) 0);
        if (type == short.class) return Short.valueOf((short) 0);
        if (type == int.class) return Integer.valueOf(0);
        if (type == long.class) return Long.valueOf(0L);
        if (type == float.class) return Float.valueOf(0F);
        if (type == double.class) return Double.valueOf(0D);
        if (type == char.class) return Character.valueOf('\0');
        return null;
    }
}
