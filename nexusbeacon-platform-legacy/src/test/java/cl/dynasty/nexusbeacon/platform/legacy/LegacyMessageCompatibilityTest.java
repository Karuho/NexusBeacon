package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

class LegacyMessageCompatibilityTest {
    private final LegacyTextFormatter formatter = new LegacyTextFormatter();

    @Test
    void preservesChatTextAndTranslatesNamedFormatting() {
        assertEquals("Hello \u00a7aLegacy \u00a7lplayer", formatter.format("Hello &aLegacy &lplayer"));
    }

    @Test
    void mapsUnsupportedHexToDeterministicNearestNamedColor() {
        assertEquals("\u00a7aGreen", formatter.format("#55ff55Green"));
        assertEquals("\u00a74Red", formatter.format("&#ff0000Red"));
    }

    @Test
    void mapsExpandedModernHexWithoutDroppingText() {
        assertEquals("\u00a7aGreen", formatter.format("\u00a7x\u00a75\u00a75\u00a7f\u00a7f\u00a75\u00a75Green"));
    }

    @Test
    void chatServiceSendsExactFormattedString() {
        AtomicReference<String> sent = new AtomicReference<String>();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(),
                new Class<?>[] { Player.class }, (instance, method, args) -> {
                    if (method.getName().equals("sendMessage") && args != null && args.length == 1
                            && args[0] instanceof String) {
                        sent.set((String) args[0]);
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    return null;
                });
        new LegacyMessageService(formatter).sendChat(player, "&bHello");
        assertEquals("\u00a7bHello", sent.get());
    }

    @Test
    void buildsCurrentClickableSuggestionWithStableLegacyComponentApi() {
        TextComponent component = new LegacyMessageService(formatter)
                .createSuggestedCommandComponent("&eClick", "/nb trust ");
        assertEquals("\u00a7f\u00a7eClick", component.toLegacyText());
        assertEquals(ClickEvent.Action.SUGGEST_COMMAND, component.getClickEvent().getAction());
        assertEquals("/nb trust ", component.getClickEvent().getValue());
    }
}
