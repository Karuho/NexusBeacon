package cl.dynasty.nexusbeacon.service;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

public interface TeleporterService {

    void teleport(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause);
}