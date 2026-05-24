package cl.dynasty.dynabeacon.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.model.BeaconData;
import cl.dynasty.dynabeacon.util.LocationUtil;

public class SqliteBeaconStorageProvider implements BeaconStorageProvider {

    private final DynaBeaconPlugin plugin;
    private final String url;

    public SqliteBeaconStorageProvider(DynaBeaconPlugin plugin) {
        this.plugin = plugin;

        String fileName = plugin.getConfigManager()
                .getConfig()
                .getString("storage.sqlite.file", "storage.db");

        File file = new File(plugin.getDataFolder(), fileName);
        this.url = "jdbc:sqlite:" + file.getAbsolutePath();

        prepareTable();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void prepareTable() {
        String sql = "CREATE TABLE IF NOT EXISTS dynabeacon_beacons ("
                + "id TEXT PRIMARY KEY,"
                + "unique_id TEXT NOT NULL,"
                + "owner TEXT,"
                + "range_value INTEGER NOT NULL,"
                + "level_value INTEGER NOT NULL,"
                + "protect_base_blocks INTEGER NOT NULL,"
                + "effects TEXT,"
                + "active_effects TEXT,"
                + "trusted TEXT"
                + ");";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo preparar SQLite.", exception);
        }
    }

    @Override
    public List<BeaconData> loadBeacons() {
        List<BeaconData> beacons = new ArrayList<>();
        String sql = "SELECT * FROM dynabeacon_beacons;";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                String id = result.getString("id");
                Location location = LocationUtil.deserialize(id);
                if (location == null) continue;

                beacons.add(new BeaconData(
                        id,
                        result.getString("unique_id"),
                        location,
                        parseUuid(result.getString("owner")),
                        result.getInt("range_value"),
                        result.getInt("level_value"),
                        deserializeEffects(result.getString("effects")),
                        deserializeStringSet(result.getString("active_effects")),
                        deserializeUuidSet(result.getString("trusted")),
                        result.getInt("protect_base_blocks") == 1
                ));
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe("No se pudieron cargar beacons desde SQLite: " + exception.getMessage());
        }

        return beacons;
    }

    @Override
    public void saveBeacon(BeaconData beacon) {
        String sql = "REPLACE INTO dynabeacon_beacons "
                + "(id, unique_id, owner, range_value, level_value, protect_base_blocks, effects, active_effects, trusted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, beacon.getId());
            statement.setString(2, beacon.getUniqueId());
            statement.setString(3, beacon.getOwner() != null ? beacon.getOwner().toString() : null);
            statement.setInt(4, beacon.getRange());
            statement.setInt(5, beacon.getLevel());
            statement.setInt(6, beacon.isProtectBaseBlocks() ? 1 : 0);
            statement.setString(7, serializeEffects(beacon.getEffectLevels()));
            statement.setString(8, serializeStrings(beacon.getActiveEffects()));
            statement.setString(9, serializeUuids(beacon.getTrustedPlayers()));

            statement.executeUpdate();

        } catch (SQLException exception) {
            plugin.getLogger().severe("No se pudo guardar beacon en SQLite: " + exception.getMessage());
        }
    }

    @Override
    public void removeBeacon(String id) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM dynabeacon_beacons WHERE id=?;")) {

            statement.setString(1, id);
            statement.executeUpdate();

        } catch (SQLException exception) {
            plugin.getLogger().severe("No se pudo eliminar beacon en SQLite: " + exception.getMessage());
        }
    }

    @Override
    public void close() {
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String serializeEffects(Map<String, Integer> effects) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : effects.entrySet()) {
            if (builder.length() > 0) builder.append(",");
            builder.append(entry.getKey()).append(":").append(entry.getValue());
        }
        return builder.toString();
    }

    private Map<String, Integer> deserializeEffects(String raw) {
        Map<String, Integer> effects = new HashMap<>();
        if (raw == null || raw.isEmpty()) return effects;

        for (String part : raw.split(",")) {
            String[] split = part.split(":");
            if (split.length != 2) continue;

            try {
                effects.put(split[0].toLowerCase(), Integer.parseInt(split[1]));
            } catch (NumberFormatException ignored) {
            }
        }

        return effects;
    }

    private String serializeStrings(Set<String> values) {
        return String.join(",", values);
    }

    private Set<String> deserializeStringSet(String raw) {
        Set<String> values = new HashSet<>();
        if (raw == null || raw.isEmpty()) return values;

        for (String part : raw.split(",")) {
            if (!part.isEmpty()) values.add(part.toLowerCase());
        }

        return values;
    }

    private String serializeUuids(Set<UUID> values) {
        List<String> raw = new ArrayList<>();
        for (UUID uuid : values) raw.add(uuid.toString());
        return String.join(",", raw);
    }

    private Set<UUID> deserializeUuidSet(String raw) {
        Set<UUID> values = new HashSet<>();
        if (raw == null || raw.isEmpty()) return values;

        for (String part : raw.split(",")) {
            UUID uuid = parseUuid(part);
            if (uuid != null) values.add(uuid);
        }

        return values;
    }
}