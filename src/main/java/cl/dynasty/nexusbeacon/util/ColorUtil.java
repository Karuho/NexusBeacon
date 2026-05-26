package cl.dynasty.nexusbeacon.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public final class ColorUtil {

    private ColorUtil() {
    }

    public static String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> color(List<String> lines) {
        List<String> colored = new ArrayList<String>();

        if (lines == null) {
            return colored;
        }

        for (String line : lines) {
            colored.add(color(line));
        }

        return colored;
    }
}