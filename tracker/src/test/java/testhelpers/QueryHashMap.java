package testhelpers;

import org.matomo.sdk.QueryParams;
import org.matomo.sdk.TrackMe;

import java.util.HashMap;


public class QueryHashMap extends HashMap<String, String> {

    public QueryHashMap(TrackMe trackMe) {
        super(trackMe.toMap());
    }

    public String get(QueryParams key) {
        return get(key.toString());
    }

}
