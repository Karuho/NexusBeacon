package cl.dynasty.nexusbeacon.platform.api;

public interface MaterialResolver {
    MaterialResolution resolveMaterial(String identifier, MaterialContext context);
}
