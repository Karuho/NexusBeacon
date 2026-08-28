package cl.dynasty.nexusbeacon.platform.classic;
import java.util.Collection;
public interface ClassicBeaconStorage { ClassicStorageLoad load(); void store(Collection<ClassicBeaconRecord> records); void close(); }
