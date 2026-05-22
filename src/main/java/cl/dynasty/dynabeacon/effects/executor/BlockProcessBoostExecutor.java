package cl.dynasty.dynabeacon.effects.executor;

import java.lang.reflect.Method;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.ConfigurationSection;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.model.BeaconData;

public class BlockProcessBoostExecutor implements EffectExecutor {

    private final DynaBeaconPlugin plugin;

    public BlockProcessBoostExecutor(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getType() {
        return "BLOCK_PROCESS_BOOST";
    }

    @Override
    public void tick(BeaconData beacon, BeaconEffect effect) {
        Location center = beacon.getLocation();
        if (center == null || center.getWorld() == null)
            return;

        ConfigurationSection section = plugin.getConfigManager()
                .getEffectsConfig()
                .getConfigurationSection("effects." + effect.getId());

        if (section == null)
            return;

        int level = Math.max(1, beacon.getEffectLevel(effect.getId()));
        int radius = section.getInt("scan-radius", Math.min(beacon.getRange(), 16));
        int verticalRadius = section.getInt("vertical-radius", radius);
        int maxBlocksPerTick = section.getInt("max-blocks-per-tick", 32);
        int maxScannedBlocks = section.getInt("max-scanned-blocks-per-tick", 2000);

        // Leemos los tiempos objetivo desde el yml.
        // Si el efecto es furnace_boost solo tendrá furnace-time-level-*.
        // Si es modern_furnace_boost solo tendrá modern-furnace-time-level-*.
        // El código elige cuál usar según isModernFurnace(material).
        int furnaceTimeL1 = section.getInt("furnace-time-level-1", 160);
        int furnaceTimeL2 = section.getInt("furnace-time-level-2", 120);
        int furnaceTimeL3 = section.getInt("furnace-time-level-3", 80);

        int modernTimeL1 = section.getInt("modern-furnace-time-level-1", 80);
        int modernTimeL2 = section.getInt("modern-furnace-time-level-2", 60);
        int modernTimeL3 = section.getInt("modern-furnace-time-level-3", 40);

        List<String> targetBlocks = section.getStringList("target-blocks");
        if (targetBlocks.isEmpty()) {
            targetBlocks = java.util.Arrays.asList("FURNACE");
        }

        int scanned = 0;
        int processed = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -verticalRadius; y <= verticalRadius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    if (processed >= maxBlocksPerTick)
                        return;

                    if (++scanned >= maxScannedBlocks)
                        return;

                    Block block = center.getWorld().getBlockAt(
                            center.getBlockX() + x,
                            center.getBlockY() + y,
                            center.getBlockZ() + z);

                    if (!isTargetBlock(block.getType(), targetBlocks))
                        continue;

                    // Verificamos instanceof antes del cast
                    if (!(block.getState() instanceof Furnace))
                        continue;

                    Furnace furnace = (Furnace) block.getState();

                    // Solo actuamos si el horno tiene combustible activo
                    // getBurnTime() > 0 significa que hay fuego activo
                    // getCookTime() >= 0 siempre es true, pero > 0 significa que está cocinando
                    if (furnace.getBurnTime() <= 0)
                        continue;

                    boolean isModern = isModernFurnace(block.getType());

                    int wantedTotal;
                    if (isModern) {
                        wantedTotal = level >= 3 ? modernTimeL3 : (level >= 2 ? modernTimeL2 : modernTimeL1);
                    } else {
                        wantedTotal = level >= 3 ? furnaceTimeL3 : (level >= 2 ? furnaceTimeL2 : furnaceTimeL1);
                    }

                    // Leemos el cookTimeTotal actual para comparar
                    int currentTotal = getCookTimeTotal(furnace);

                    plugin.getLogger().info("[DynaBeacon] Horno encontrado: "
                            + block.getType()
                            + " isModern=" + isModern
                            + " cookTime=" + furnace.getCookTime()
                            + " burnTime=" + furnace.getBurnTime()
                            + " cookTimeTotal(actual)=" + currentTotal
                            + " wantedTotal=" + wantedTotal
                            + " nivel=" + level);

                    // Solo modificamos si el tiempo total actual es mayor al objetivo.
                    // Esto evita escribir en cada tick si ya está al valor correcto.
                    if (currentTotal > wantedTotal) {
                        boolean totalSet = setCookTimeTotal(furnace, wantedTotal);

                        plugin.getLogger().info("[DynaBeacon] setCookTimeTotal result=" + totalSet
                                + " -> wantedTotal=" + wantedTotal);

                        if (!totalSet) {
                            // Fallback: avanzamos manualmente el cookTime.
                            // La lógica correcta es: adelantar el progreso actual
                            // proporcionalmente al ratio de aceleración deseado.
                            // Si vanilla tarda 200 y queremos 80, el ratio es 200/80 = 2.5x
                            // Cada tick del beacon (~40 ticks de juego) avanzamos extra.
                            int vanillaTotal = isModern ? 100 : 200;
                            int ratio = Math.max(1, vanillaTotal / wantedTotal);
                            short currentCook = furnace.getCookTime();
                            // Avanzamos (ratio - 1) ticks extra por cada tick de juego
                            // multiplicado por el intervalo del beacon (40 ticks)
                            int advance = (ratio - 1) * 40;
                            short newCook = (short) Math.min(wantedTotal - 1, currentCook + advance);
                            furnace.setCookTime(newCook);

                            plugin.getLogger().info("[DynaBeacon] Fallback: cookTime " + currentCook + " -> " + newCook
                                    + " (advance=" + advance + " ratio=" + ratio + ")");
                        }
                    }

                    // Mantenemos el combustible para no cortar el ciclo
                    if (furnace.getBurnTime() < 60) {
                        furnace.setBurnTime((short) 60);
                    }

                    furnace.update(true);
                    processed++;
                }
            }
        }
    }

    /**
     * Lee el cookTimeTotal actual del horno via reflection.
     * Devuelve el valor vanilla (200 para FURNACE, 100 para modernos) si no existe el método.
     */
    private int getCookTimeTotal(Furnace furnace) {
        try {
            Method method = furnace.getClass().getMethod("getCookTimeTotal");
            Object result = method.invoke(furnace);
            return result instanceof Number ? ((Number) result).intValue() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Setea el cookTimeTotal via reflection.
     * @return true si tuvo éxito, false si el método no existe o falló.
     */
    private boolean setCookTimeTotal(Furnace furnace, int totalTime) {
        try {
            Method method = furnace.getClass().getMethod("setCookTimeTotal", int.class);
            method.invoke(furnace, totalTime);
            return true;
        } catch (NoSuchMethodException e) {
            plugin.getLogger().warning("[DynaBeacon] setCookTimeTotal no disponible en esta versión del servidor.");
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("[DynaBeacon] setCookTimeTotal falló: " + e.getMessage());
            return false;
        }
    }

    private boolean isModernFurnace(Material material) {
        String name = material.name();
        return name.equals("BLAST_FURNACE") || name.equals("SMOKER");
    }

    private boolean isTargetBlock(Material material, List<String> targetBlocks) {
        for (String target : targetBlocks) {
            Material targetMaterial = plugin.getVersionAdapter().material(target);
            if (targetMaterial == material)
                return true;
        }
        return false;
    }
}