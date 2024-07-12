package testhelpers

import org.matomo.sdk.QueryParams
import org.matomo.sdk.TrackMe

class QueryHashMap(trackMe: TrackMe) : HashMap<String?, String?>(trackMe.toMap()) {
    fun get(key: QueryParams): String? {
        return get(key.toString())
    }
}
