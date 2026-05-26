package cl.dynasty.nexusbeacon.task;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.beam.BeamRenderer;
import cl.dynasty.nexusbeacon.model.BeaconData;

public final class BeaconVisualBeamTask implements Runnable {

    private final NexusBeaconPlugin plugin;
    private final BeamRenderer renderer;

    public BeaconVisualBeamTask(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
        this.renderer = new BeamRenderer(plugin);
    }

    @Override
    public void run() {
        if (!plugin.getConfigManager().getBeaconConfig().getBoolean("visual-beam.enabled", true)) {
            return;
        }
       for (BeaconData beacon : plugin.getBeaconManager().getBeacons()) {
            renderer.render(beacon);
        }
    }
}