package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.Material;

final class LegacyPaymentOption {
    enum Type { NONE, ITEM, EXP_LEVEL, VAULT_MONEY }
    private final Type type;
    private final int amount;
    private final Material material;

    LegacyPaymentOption(Type type, int amount, Material material) {
        this.type = type;
        this.amount = amount;
        this.material = material;
    }
    Type getType() { return type; }
    int getAmount() { return amount; }
    Material getMaterial() { return material; }
}
