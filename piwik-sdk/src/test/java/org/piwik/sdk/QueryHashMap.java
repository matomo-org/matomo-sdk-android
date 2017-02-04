package org.piwik.sdk;

import java.util.HashMap;


public class QueryHashMap extends HashMap<String, String> {

    QueryHashMap(TrackMe trackMe) {
        super(trackMe.toMap());
    }

    public String get(QueryParams key) {
        return get(key.toString());
    }

    public boolean containsKey(QueryParams key) {
        return super.containsKey(key.toString());
    }
}
