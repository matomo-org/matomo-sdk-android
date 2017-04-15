/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk.dispatcher;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.piwik.sdk.Piwik;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;


public class PacketFactory {
    private static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "PacketFactory";
    @VisibleForTesting
    public static final int PAGE_SIZE = 20;
    private final URL mApiUrl;

    public PacketFactory(@NonNull final URL apiUrl) {
        mApiUrl = apiUrl;
    }

    @NonNull
    public List<Packet> buildPackets(@NonNull final List<Event> events) {
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
            Timber.tag(LOGGER_TAG).w(e, "Cannot create json object:\n%s", TextUtils.join(", ", events));
        }
        return null;
    }

    // "http://domain.com/piwik.php?idsite=1&url=http://a.org&action_name=Test bulk log Pageview&rec=1"
    @Nullable
    private Packet buildPacketForGet(@NonNull Event event) {
        if (event.getEncodedQuery().isEmpty()) return null;
        try {
            return new Packet(new URL(mApiUrl.toString() + event));
        } catch (MalformedURLException e) {
            Timber.tag(LOGGER_TAG).w(e, null);
        }
        return null;
    }

}
