package org.piwik.sdk.testhelper;

import org.piwik.sdk.QueryParams;
import org.piwik.sdk.TrackMe;

import java.util.HashMap;


public class QueryHashMap extends HashMap<String, String> {

    public QueryHashMap(TrackMe trackMe) {
        super(trackMe.toMap());
    }

    public String get(QueryParams key) {
        return get(key.toString());
    }

    public boolean containsKey(QueryParams key) {
        return super.containsKey(key.toString());
    }
}
