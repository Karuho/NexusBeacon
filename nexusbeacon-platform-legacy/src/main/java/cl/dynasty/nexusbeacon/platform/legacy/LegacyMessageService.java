package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

/** Stable chat operations used by current GUI/application paths. */
public final class LegacyMessageService {
    private final LegacyTextFormatter formatter;

    public LegacyMessageService(LegacyTextFormatter formatter) {
        if (formatter == null) throw new NullPointerException("formatter");
        this.formatter = formatter;
    }

    public void sendChat(Player player, String text) {
        if (player == null) return;
        player.sendMessage(formatter.format(text));
    }

    public void sendSuggestedCommand(Player player, String text, String command) {
        if (player == null) return;
        player.spigot().sendMessage(createSuggestedCommandComponent(text, command));
    }

    TextComponent createSuggestedCommandComponent(String text, String command) {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("suggested command cannot be empty");
        }
        TextComponent component = new TextComponent(formatter.format(text));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        return component;
    }
}
