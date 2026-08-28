package cl.dynasty.nexusbeacon.platform.classic;
import java.util.ArrayList; import java.util.Collections; import java.util.List;
public final class ClassicStorageLoad {
    public enum Status { SUCCESS, UNAVAILABLE, CORRUPT }
    private final Status status; private final List<ClassicBeaconRecord> records; private final String diagnostic;
    private ClassicStorageLoad(Status s,List<ClassicBeaconRecord> r,String d){status=s;records=Collections.unmodifiableList(new ArrayList<ClassicBeaconRecord>(r));diagnostic=d;}
    public static ClassicStorageLoad success(List<ClassicBeaconRecord> r){return new ClassicStorageLoad(Status.SUCCESS,r,null);}
    public static ClassicStorageLoad failure(Status s,String d){return new ClassicStorageLoad(s,Collections.<ClassicBeaconRecord>emptyList(),d);}
    public boolean isSuccessful(){return status==Status.SUCCESS;} public Status getStatus(){return status;} public List<ClassicBeaconRecord> getRecords(){return records;} public String getDiagnostic(){return diagnostic;}
}
