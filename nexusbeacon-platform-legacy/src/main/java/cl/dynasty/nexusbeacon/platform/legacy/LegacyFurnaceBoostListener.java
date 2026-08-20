package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;

import cl.dynasty.nexusbeacon.platform.api.SchedulerService;

/** Modern-equivalent furnace event bridge; the effect/runtime remains authoritative. */
public final class LegacyFurnaceBoostListener implements Listener {
    private final LegacyEffectRuntime effects;
    private final SchedulerService scheduler;

    public LegacyFurnaceBoostListener(LegacyEffectRuntime effects, SchedulerService scheduler) {
        if (effects == null || scheduler == null) throw new NullPointerException();
        this.effects = effects;
        this.scheduler = scheduler;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        LegacyFurnaceBoost boost = effects.findBestFurnaceBoost(event.getBlock());
        if (boost == null) return;
        int reduction = (int) (event.getBurnTime() * (boost.getFuelPercent() / 100.0D));
        event.setBurnTime(Math.max(1, event.getBurnTime() - reduction));
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        final LegacyFurnaceBoost boost = effects.findBestFurnaceBoost(event.getBlock());
        if (boost == null) return;
        final Block block = event.getBlock();
        scheduler.runSync(new Runnable() {
            @Override public void run() {
                if (!(block.getState() instanceof Furnace)) return;
                Furnace furnace = (Furnace) block.getState();
                if (furnace.getInventory().getSmelting() == null) return;
                int total = block.getType() == Material.FURNACE ? 200 : 100;
                int add = (int) (total * (boost.getCookPercent() / 100.0D));
                furnace.setCookTime((short) Math.min(total, furnace.getCookTime() + add));
                furnace.update(true);
            }
        });
    }
}
