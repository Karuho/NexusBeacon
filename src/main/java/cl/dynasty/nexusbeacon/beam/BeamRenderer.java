package cl.dynasty.nexusbeacon.beam;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.model.BeaconData;
import cl.dynasty.nexusbeacon.platform.api.VerticalBeamGeometry;

public final class BeamRenderer {

    private boolean shouldRender(BeaconData beacon) {
        String renderMode = plugin.getConfigManager().getBeaconConfig()
                .getString("visual-beam.render-mode", "AUTO");

        boolean customBase = plugin.getBeaconPowerManager().usesCustomPowerBlock(beacon);

        if (renderMode.equalsIgnoreCase("ALWAYS")) {
            return true;
        }

        if (renderMode.equalsIgnoreCase("CUSTOM_ONLY")) {
            return customBase;
        }

        return customBase;
    }

    private final NexusBeaconPlugin plugin;

    public BeamRenderer(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public void render(BeaconData beacon) {
        if (beacon == null || beacon.getLocation() == null || beacon.getLocation().getWorld() == null) {
            return;
        }

        if (!shouldRender(beacon)) {
            return;
        }

        BeamStyle style = resolveStyle(beacon);

        if (style == null) {
            return;
        }

        Location base = beacon.getLocation().clone().add(0.5, 1.0, 0.5);
        World world = base.getWorld();

        int height = renderHeight(beacon);
        double step = Math.max(0.25D, plugin.getConfigManager().getBeaconConfig().getDouble("visual-beam.step", 0.45));
        int count = Math.max(1, plugin.getConfigManager().getBeaconConfig().getInt("visual-beam.count", 1));

        Particle particle = style.getParticle();

        VerticalBeamGeometry.forEachPoint(base, height, step, point -> {
            if (style.getColor() != null && particle == Particle.DUST) {
                Particle.DustOptions dust = new Particle.DustOptions(style.getColor(), style.getSize());
                world.spawnParticle(particle, point, count, 0, 0, 0, 0, dust);
            } else {
                world.spawnParticle(particle, point, count, 0, 0, 0, 0);
            }
        });
    }

    public void renderForPlayer(Player player, BeaconData beacon) {
        if (player == null || beacon == null || beacon.getLocation() == null
                || beacon.getLocation().getWorld() == null) {
            return;
        }

        BeamStyle style = resolveStyle(beacon);

        if (style == null) {
            return;
        }

        Location base = beacon.getLocation().clone().add(0.5, 1.0, 0.5);

        int height = renderHeight(beacon);
        double step = Math.max(0.25D, plugin.getConfigManager().getBeaconConfig().getDouble("visual-beam.step", 0.45));
        int count = Math.max(1, plugin.getConfigManager().getBeaconConfig().getInt("visual-beam.count", 1));
        Particle particle = style.getParticle();

        VerticalBeamGeometry.forEachPoint(base, height, step, point -> {
            if (style.getColor() != null && particle == Particle.DUST) {
                Particle.DustOptions dust = new Particle.DustOptions(style.getColor(), style.getSize());
                player.spawnParticle(particle, point, count, 0, 0, 0, 0, dust);
            } else {
                player.spawnParticle(particle, point, count, 0, 0, 0, 0);
            }
        });
    }

    private BeamStyle resolveStyle(BeaconData beacon) {
        String styleMode = plugin.getConfigManager().getBeaconConfig()
                .getString("visual-beam.style-mode", "PLAYER_OR_GLOBAL");

        if (styleMode.equalsIgnoreCase("GLOBAL")) {
            return plugin.getBeamStyleManager().getGlobalStyle();
        }

        if (styleMode.equalsIgnoreCase("PLAYER") || styleMode.equalsIgnoreCase("PLAYER_OR_GLOBAL")) {
            BeamStyle playerStyle = plugin.getBeamStyleManager().getStyle(beacon.getBeamStyle());

            if (playerStyle != null) {
                return playerStyle;
            }

            if (styleMode.equalsIgnoreCase("PLAYER")) {
                return null;
            }
        }

        return plugin.getBeamStyleManager().getGlobalStyle();
    }

    private int renderHeight(BeaconData beacon) {
        var config = plugin.getConfigManager().getBeaconConfig();
        return ModernBeamHeight.height(
                ModernBeamHeight.mode(config),
                config.getInt("visual-beam.height", 96),
                beacon.getLocation().getBlockY(),
                beacon.getLocation().getWorld().getMaxHeight());
    }
}
