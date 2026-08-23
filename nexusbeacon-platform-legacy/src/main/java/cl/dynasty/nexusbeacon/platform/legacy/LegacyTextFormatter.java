package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;

/** Converts current text intent to the named-color format supported by Legacy clients. */
public final class LegacyTextFormatter {
    private static final Pattern HEX = Pattern.compile("(?i)(?:&#|#)([0-9a-f]{6})");
    private static final Pattern EXPANDED_HEX = Pattern.compile(
            "(?i)\\u00a7x\\u00a7([0-9a-f])\\u00a7([0-9a-f])\\u00a7([0-9a-f])\\u00a7([0-9a-f])\\u00a7([0-9a-f])\\u00a7([0-9a-f])");
    private static final LegacyColor[] COLORS = {
            new LegacyColor('0', 0x000000), new LegacyColor('1', 0x0000AA),
            new LegacyColor('2', 0x00AA00), new LegacyColor('3', 0x00AAAA),
            new LegacyColor('4', 0xAA0000), new LegacyColor('5', 0xAA00AA),
            new LegacyColor('6', 0xFFAA00), new LegacyColor('7', 0xAAAAAA),
            new LegacyColor('8', 0x555555), new LegacyColor('9', 0x5555FF),
            new LegacyColor('a', 0x55FF55), new LegacyColor('b', 0x55FFFF),
            new LegacyColor('c', 0xFF5555), new LegacyColor('d', 0xFF55FF),
            new LegacyColor('e', 0xFFFF55), new LegacyColor('f', 0xFFFFFF)
    };

    public String format(String input) {
        if (input == null) return null;
        String named = replaceExpandedHex(input);
        Matcher matcher = HEX.matcher(named);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(nearest(matcher.group(1))));
        }
        matcher.appendTail(result);
        return ChatColor.translateAlternateColorCodes('&', result.toString());
    }

    private String replaceExpandedHex(String input) {
        Matcher matcher = EXPANDED_HEX.matcher(input);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            StringBuilder hex = new StringBuilder(6);
            for (int index = 1; index <= 6; index++) hex.append(matcher.group(index));
            matcher.appendReplacement(result, Matcher.quoteReplacement(nearest(hex.toString())));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String nearest(String hex) {
        int rgb = Integer.parseInt(hex, 16);
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        LegacyColor best = COLORS[0];
        int bestDistance = Integer.MAX_VALUE;
        for (LegacyColor candidate : COLORS) {
            int cr = (candidate.rgb >> 16) & 0xff;
            int cg = (candidate.rgb >> 8) & 0xff;
            int cb = candidate.rgb & 0xff;
            int distance = square(red - cr) + square(green - cg) + square(blue - cb);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return "\u00a7" + best.code;
    }

    private static int square(int value) { return value * value; }

    private static final class LegacyColor {
        private final char code;
        private final int rgb;
        private LegacyColor(char code, int rgb) { this.code = code; this.rgb = rgb; }
    }
}
