package cl.dynasty.nexusbeacon.storage;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.model.BeaconData;
import cl.dynasty.nexusbeacon.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class YamlBeaconStorageProvider implements BeaconStorageProvider {

    private final NexusBeaconPlugin plugin;

    public YamlBeaconStorageProvider(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public List<BeaconData> loadBeacons() {
        List<BeaconData> beacons = new ArrayList<BeaconData>();
        FileConfiguration storage = plugin.getConfigManager().getStorageConfig();

        ConfigurationSection section = storage.getConfigurationSection("beacons");
        if (section == null)
            return beacons;

        for (String id : section.getKeys(false)) {
            Location location = LocationUtil.deserialize(id);
            if (location == null)
                continue;

            String uniqueId = section.getString(id + ".unique-id", UUID.randomUUID().toString());

            UUID owner = null;
            String ownerRaw = section.getString(id + ".owner", null);
            try {
                if (ownerRaw != null && !ownerRaw.isEmpty())
                    owner = UUID.fromString(ownerRaw);
            } catch (IllegalArgumentException ignored) {
            }

            int range = section.getInt(id + ".range", 100);
            int level = section.getInt(id + ".level", 1);

            Map<String, Integer> effects = new HashMap<String, Integer>();
            ConfigurationSection effectsSection = section.getConfigurationSection(id + ".effects");
            if (effectsSection != null) {
                for (String effectId : effectsSection.getKeys(false)) {
                    effects.put(effectId.toLowerCase(), effectsSection.getInt(effectId, 1));
                }
            }

            Set<String> activeEffects = new HashSet<String>();
            for (String effectId : section.getStringList(id + ".active-effects")) {
                activeEffects.add(effectId.toLowerCase());
            }

            Set<UUID> trustedPlayers = new HashSet<UUID>();

            for (String rawUuid : section.getStringList(id + ".trusted")) {
                try {
                    trustedPlayers.add(UUID.fromString(rawUuid));
                } catch (IllegalArgumentException ignored) {
                }
            }

            boolean protectBaseBlocks = section.getBoolean(id + ".protect-base-blocks", true);
            boolean rangeParticlesEnabled = section.getBoolean(id + ".range-particles-enabled", true);
            String rangeParticleType = section.getString(id + ".range-particle-type", "VILLAGER_HAPPY");
            String beamStyle = section.getString(id + ".beam-style", null);

            BeaconData beacon = new BeaconData(id, uniqueId, location, owner, range, level, effects, activeEffects,
                    trustedPlayers, protectBaseBlocks, beamStyle);

            beacon.setRangeParticlesEnabled(rangeParticlesEnabled);
            beacon.setRangeParticleType(rangeParticleType);

            beacons.add(beacon);

            beacons.add(new BeaconData(id, uniqueId, location, owner, range, level, effects, activeEffects,
                    trustedPlayers, protectBaseBlocks, null));
        }

        return beacons;
    }

    public void saveBeacon(BeaconData beacon) {
        FileConfiguration storage = plugin.getConfigManager().getStorageConfig();
        String id = beacon.getId();

        List<String> trusted = new ArrayList<String>();

        for (UUID uuid : beacon.getTrustedPlayers()) {
            trusted.add(uuid.toString());
        }

        storage.set("beacons." + id + ".trusted", trusted);
        storage.set("beacons." + id + ".protect-base-blocks", beacon.isProtectBaseBlocks());
        storage.set("beacons." + id + ".unique-id", beacon.getUniqueId());
        storage.set("beacons." + id + ".owner", beacon.getOwner() != null ? beacon.getOwner().toString() : "");
        storage.set("beacons." + id + ".range", beacon.getRange());
        storage.set("beacons." + id + ".level", beacon.getLevel());
        storage.set("beacons." + id + ".effects", beacon.getEffectLevels());
        storage.set("beacons." + id + ".active-effects", new ArrayList<String>(beacon.getActiveEffects()));
        storage.set("beacons." + id + ".range-particles-enabled", beacon.isRangeParticlesEnabled());
        storage.set("beacons." + id + ".range-particle-type", beacon.getRangeParticleType());

        plugin.getConfigManager().saveStorage();
    }

    public void removeBeacon(String id) {
        FileConfiguration storage = plugin.getConfigManager().getStorageConfig();
        storage.set("beacons." + id, null);
        plugin.getConfigManager().saveStorage();
    }

    @Override
    public void close() {
        if (plugin.getConfigManager().getStorageConfig() != null) {
            plugin.getConfigManager().saveStorage();
        }
    }

}