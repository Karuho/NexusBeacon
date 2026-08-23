package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.Material;

public final class LegacyRecipeIngredient {
    private final char symbol;
    private final String identifier;
    private final Material material;
    private final short data;
    private final LegacyMaterialMappingKind mappingKind;

    LegacyRecipeIngredient(char symbol, String identifier, Material material, short data,
            LegacyMaterialMappingKind mappingKind) {
        this.symbol = symbol;
        this.identifier = identifier;
        this.material = material;
        this.data = data;
        this.mappingKind = mappingKind;
    }

    public char getSymbol() { return symbol; }
    public String getIdentifier() { return identifier; }
    public Material getMaterial() { return material; }
    public short getData() { return data; }
    public LegacyMaterialMappingKind getMappingKind() { return mappingKind; }
}
