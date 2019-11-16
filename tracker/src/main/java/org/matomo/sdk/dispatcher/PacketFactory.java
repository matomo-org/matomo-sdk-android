/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.matomo.sdk.dispatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.matomo.sdk.Matomo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;


public class PacketFactory {
    private static final String TAG = Matomo.tag(PacketFactory.class);
    @VisibleForTesting
    public static final int PAGE_SIZE = 20;
    private final String mApiUrl;

    public PacketFactory(final String apiUrl) {
        mApiUrl = apiUrl;
    }

    public List<Packet> buildPackets(final List<Event> events) {
        if (events.isEmpty()) return Collections.emptyList();

        if (events.size() == 1) {
            Packet p = buildPacketForGet(events.get(0));
            if (p == null) return Collections.emptyList();
            else return Collections.singletonList(p);
        }

        int packets = (int) Math.ceil(events.size() * 1.0 / PAGE_SIZE);
        List<Packet> freshPackets = new ArrayList<>(packets);
        for (int i = 0; i < events.size(); i += PAGE_SIZE) {
            List<Event> batch = events.subList(i, Math.min(i + PAGE_SIZE, events.size()));
            final Packet packet;
            if (batch.size() == 1) packet = buildPacketForGet(batch.get(0));
            else packet = buildPacketForPost(batch);
            if (packet != null) freshPackets.add(packet);
        }
        return freshPackets;
    }

    //{
    //    "requests": ["?idsite=1&url=http://example.org&action_name=Test bulk log Pageview&rec=1",
    //    "?idsite=1&url=http://example.net/test.htm&action_name=Another bul k page view&rec=1"]
    //}
    @Nullable
    private Packet buildPacketForPost(List<Event> events) {
        if (events.isEmpty()) return null;
        try {
            JSONObject params = new JSONObject();

            JSONArray jsonArray = new JSONArray();
            for (Event event : events) jsonArray.put(event.getEncodedQuery());
            params.put("requests", jsonArray);
            return new Packet(mApiUrl, params, events.size());
        } catch (JSONException e) {
            Timber.tag(TAG).w(e, "Cannot create json object:\n%s", TextUtils.join(", ", events));
        }
        return null;
    }

    // "http://domain.com/matomo.php?idsite=1&url=http://a.org&action_name=Test bulk log Pageview&rec=1"
    @Nullable
    private Packet buildPacketForGet(@NonNull Event event) {
        if (event.getEncodedQuery().isEmpty()) return null;
        return new Packet(mApiUrl + event);
    }

}
