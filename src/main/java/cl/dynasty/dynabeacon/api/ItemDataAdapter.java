package cl.dynasty.dynabeacon.api;

import cl.dynasty.dynabeacon.model.BeaconData;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;

public interface ItemDataAdapter {

    ItemStack writeBaseMarker(ItemStack item);

    ItemStack writeBeaconData(ItemStack item, BeaconData beacon);

    boolean isCustomBeacon(ItemStack item);

    String readUniqueId(ItemStack item);

    Map<String, Integer> readEffects(ItemStack item);

    Set<String> readActiveEffects(ItemStack item);
}