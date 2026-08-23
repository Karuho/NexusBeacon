package cl.dynasty.nexusbeacon.platform.legacy;

import cl.dynasty.nexusbeacon.platform.api.MaterialResolution;

public final class LegacyMaterialResolution {
    private final MaterialResolution resolution;
    private final short data;
    private final LegacyMaterialMappingKind mappingKind;

    LegacyMaterialResolution(MaterialResolution resolution, short data, LegacyMaterialMappingKind mappingKind) {
        this.resolution = resolution;
        this.data = data;
        this.mappingKind = mappingKind;
    }

    public MaterialResolution getResolution() { return resolution; }
    public short getData() { return data; }
    public LegacyMaterialMappingKind getMappingKind() { return mappingKind; }
}
