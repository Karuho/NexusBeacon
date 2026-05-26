package cl.dynasty.nexusbeacon.storage;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.model.BeaconData;
import cl.dynasty.nexusbeacon.util.LocationUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.*;

public class MySqlBeaconStorageProvider implements BeaconStorageProvider {

    private final NexusBeaconPlugin plugin;
    private final HikariDataSource dataSource;
    private final String table;

    public MySqlBeaconStorageProvider(NexusBeaconPlugin plugin) {
        this.plugin = plugin;

        FileConfiguration config = plugin.getConfigManager().getConfig();

        String host = config.getString("storage.mysql.host", "localhost");
        int port = config.getInt("storage.mysql.port", 3306);
        String database = config.getString("storage.mysql.database", "NexusBeacon");
        String username = config.getString("storage.mysql.username", "root");
        String password = config.getString("storage.mysql.password", "");
        boolean useSsl = config.getBoolean("storage.mysql.use-ssl", false);
        String prefix = config.getString("storage.mysql.table-prefix", "NexusBeacon_");

        this.table = prefix + "beacons";

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSsl
                + "&allowPublicKeyRetrieval=true"
                + "&characterEncoding=utf8"
                + "&useUnicode=true");
        hikari.setUsername(username);
        hikari.setPassword(password);
        hikari.setMaximumPoolSize(5);
        hikari.setMinimumIdle(1);
        hikari.setPoolName("NexusBeacon-MySQL");

        this.dataSource = new HikariDataSource(hikari);

        prepareTable();
    }

    private void prepareTable() {
        String sql = "CREATE TABLE IF NOT EXISTS `" + table + "` ("
                + "`id` VARCHAR(128) NOT NULL,"
                + "`unique_id` VARCHAR(64) NOT NULL,"
                + "`world` VARCHAR(64) NOT NULL,"
                + "`x` INT NOT NULL,"
                + "`y` INT NOT NULL,"
                + "`z` INT NOT NULL,"
                + "`owner` VARCHAR(64),"
                + "`range_value` INT NOT NULL,"
                + "`level_value` INT NOT NULL,"
                + "`protect_base_blocks` BOOLEAN NOT NULL,"
                + "`effects` TEXT,"
                + "`active_effects` TEXT,"
                + "`trusted` TEXT,"
                + "PRIMARY KEY (`id`)"
                + ");";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo preparar la tabla MySQL de NexusBeacon.", exception);
        }
    }

    @Override
    public List<BeaconData> loadBeacons() {
        List<BeaconData> beacons = new ArrayList<>();

        String sql = "SELECT * FROM `" + table + "`;";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                String id = result.getString("id");
                Location location = LocationUtil.deserialize(id);

                if (location == null) {
                    continue;
                }

                String uniqueId = result.getString("unique_id");

                UUID owner = parseUuid(result.getString("owner"));
                int range = result.getInt("range_value");
                int level = result.getInt("level_value");
                boolean protectBaseBlocks = result.getBoolean("protect_base_blocks");

                Map<String, Integer> effects = deserializeEffects(result.getString("effects"));
                Set<String> activeEffects = deserializeStringSet(result.getString("active_effects"));
                Set<UUID> trusted = deserializeUuidSet(result.getString("trusted"));

                beacons.add(new BeaconData(
                        id,
                        uniqueId,
                        location,
                        owner,
                        range,
                        level,
                        effects,
                        activeEffects,
                        trusted,
                        protectBaseBlocks,
                        null
                ));
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe("No se pudieron cargar beacons desde MySQL: " + exception.getMessage());
        }

        return beacons;
    }

    @Override
    public void saveBeacon(BeaconData beacon) {
        String sql = "REPLACE INTO `" + table + "` "
                + "(`id`, `unique_id`, `world`, `x`, `y`, `z`, `owner`, `range_value`, `level_value`, "
                + "`protect_base_blocks`, `effects`, `active_effects`, `trusted`) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        Location location = beacon.getLocation();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, beacon.getId());
            statement.setString(2, beacon.getUniqueId());
            statement.setString(3, location.getWorld().getName());
            statement.setInt(4, location.getBlockX());
            statement.setInt(5, location.getBlockY());
            statement.setInt(6, location.getBlockZ());
            statement.setString(7, beacon.getOwner() != null ? beacon.getOwner().toString() : null);
            statement.setInt(8, beacon.getRange());
            statement.setInt(9, beacon.getLevel());
            statement.setBoolean(10, beacon.isProtectBaseBlocks());
            statement.setString(11, serializeEffects(beacon.getEffectLevels()));
            statement.setString(12, serializeStrings(beacon.getActiveEffects()));
            statement.setString(13, serializeUuids(beacon.getTrustedPlayers()));

            statement.executeUpdate();

        } catch (SQLException exception) {
            plugin.getLogger().severe("No se pudo guardar beacon en MySQL: " + exception.getMessage());
        }
    }

    @Override
    public void removeBeacon(String id) {
        String sql = "DELETE FROM `" + table + "` WHERE `id`=?;";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);
            statement.executeUpdate();

        } catch (SQLException exception) {
            plugin.getLogger().severe("No se pudo eliminar beacon en MySQL: " + exception.getMessage());
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String serializeEffects(Map<String, Integer> effects) {
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, Integer> entry : effects.entrySet()) {
            if (builder.length() > 0) {
                builder.append(",");
            }

            builder.append(entry.getKey()).append(":").append(entry.getValue());
        }

        return builder.toString();
    }

    private Map<String, Integer> deserializeEffects(String raw) {
        Map<String, Integer> effects = new HashMap<>();

        if (raw == null || raw.isEmpty()) {
            return effects;
        }

        for (String part : raw.split(",")) {
            String[] split = part.split(":");

            if (split.length != 2) {
                continue;
            }

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

        if (raw == null || raw.isEmpty()) {
            return values;
        }

        for (String part : raw.split(",")) {
            if (!part.isEmpty()) {
                values.add(part.toLowerCase());
            }
        }

        return values;
    }

    private String serializeUuids(Set<UUID> values) {
        List<String> raw = new ArrayList<>();

        for (UUID uuid : values) {
            raw.add(uuid.toString());
        }

        return String.join(",", raw);
    }

    private Set<UUID> deserializeUuidSet(String raw) {
        Set<UUID> values = new HashSet<>();

        if (raw == null || raw.isEmpty()) {
            return values;
        }

        for (String part : raw.split(",")) {
            UUID uuid = parseUuid(part);

            if (uuid != null) {
                values.add(uuid);
            }
        }

        return values;
    }
}