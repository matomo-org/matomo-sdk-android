package org.piwik.sdk;


import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressLint("CommitPrefEdits")
public class LegacySettingsPorterTest {
    @Mock Piwik mPiwik;
    private LegacySettingsPorter mPorter;
    @Mock SharedPreferences mPiwikPrefs;
    @Mock SharedPreferences.Editor mPiwikPrefsEditor;
    @Mock SharedPreferences mTrackerPrefs;
    @Mock SharedPreferences.Editor mTrackerPrefsEditor;
    @Mock Tracker mTracker;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mPiwikPrefs.edit()).thenReturn(mPiwikPrefsEditor);
        when(mPiwikPrefsEditor.remove(anyString())).thenReturn(mPiwikPrefsEditor);

        when(mTrackerPrefs.edit()).thenReturn(mTrackerPrefsEditor);
        when(mTrackerPrefsEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mTrackerPrefsEditor);
        when(mTrackerPrefsEditor.putLong(anyString(), anyLong())).thenReturn(mTrackerPrefsEditor);
        when(mTrackerPrefsEditor.putInt(anyString(), anyInt())).thenReturn(mTrackerPrefsEditor);
        when(mTrackerPrefsEditor.putString(anyString(), anyString())).thenReturn(mTrackerPrefsEditor);

        when(mPiwik.getPiwikPreferences()).thenReturn(mPiwikPrefs);
        when(mTracker.getPreferences()).thenReturn(mTrackerPrefs);
        mPorter = new LegacySettingsPorter(mPiwik);
    }

    @Test
    public void testPort_optOut_empty() {
        when(mPiwikPrefs.getBoolean(LegacySettingsPorter.LEGACY_PREF_OPT_OUT, false)).thenReturn(false);
        mPorter.port(mTracker);

        verify(mTrackerPrefs, never()).edit();
        verify(mPiwikPrefs, never()).edit();
    }

    @Test
    public void testPort_optOut_exists() {
        when(mPiwikPrefs.getBoolean(LegacySettingsPorter.LEGACY_PREF_OPT_OUT, false)).thenReturn(true);
        mPorter.port(mTracker);

        verify(mPiwikPrefs).getBoolean(LegacySettingsPorter.LEGACY_PREF_OPT_OUT, false);
        verify(mTrackerPrefs).edit();
        verify(mTrackerPrefsEditor).putBoolean(Tracker.PREF_KEY_TRACKER_OPTOUT, true);
        verify(mPiwikPrefsEditor).remove(LegacySettingsPorter.LEGACY_PREF_OPT_OUT);
    }

    @Test
    public void testPort_userId_empty() {
        when(mPiwikPrefs.contains(LegacySettingsPorter.LEGACY_PREF_USER_ID)).thenReturn(false);
        mPorter.port(mTracker);

        verify(mTrackerPrefs, never()).edit();
        verify(mPiwikPrefs, never()).edit();
    }

    @Test
    public void testPort_userId_exists() {
        when(mPiwikPrefs.contains(LegacySettingsPorter.LEGACY_PREF_USER_ID)).thenReturn(true);
        when(mPiwikPrefs.getString(eq(LegacySettingsPorter.LEGACY_PREF_USER_ID), anyString())).thenReturn("test");
        mPorter.port(mTracker);

        verify(mPiwikPrefs).getString(eq(LegacySettingsPorter.LEGACY_PREF_USER_ID), anyString());
        verify(mTrackerPrefs).edit();
        verify(mTrackerPrefsEditor).putString(Tracker.PREF_KEY_TRACKER_USERID, "test");
        verify(mPiwikPrefsEditor).remove(LegacySettingsPorter.LEGACY_PREF_USER_ID);
    }

    @Test
    public void testPort_firstVisit_empty() {
        when(mPiwikPrefs.contains(LegacySettingsPorter.LEGACY_PREF_FIRST_VISIT)).thenReturn(false);
        mPorter.port(mTracker);

        verify(mTrackerPrefs, never()).edit();
        verify(mPiwikPrefs, never()).edit();
    }

    @Test
    public void testPort_firstVisit_exists() {
        when(mPiwikPrefs.contains(LegacySettingsPorter.LEGACY_PREF_FIRST_VISIT)).thenReturn(true);
        when(mPiwikPrefs.getLong(LegacySettingsPorter.LEGACY_PREF_FIRST_VISIT, -1L)).thenReturn(1338L);
        mPorter.port(mTracker);

        verify(mPiwikPrefs).getLong(LegacySettingsPorter.LEGACY_PREF_FIRST_VISIT, -1L);
        verify(mTrackerPrefs).edit();
        verify(mTrackerPrefsEditor).putLong(Tracker.PREF_KEY_TRACKER_FIRSTVISIT, 1338);
        verify(mPiwikPrefsEditor).remove(LegacySettingsPorter.LEGACY_PREF_FIRST_VISIT);
    }

    @Test
    public void testPort_visitCount_empty() {
        when(mPiwikPrefs.contains(LegacySettingsPorter.LEGACY_PREF_VISITCOUNT)).thenReturn(false);
        mPorter.port(mTracker);

        verify(mTrackerPrefs, never()).edit();
        verify(mPiwikPrefs, never()).edit();
    }

    @Test
    public void testPort_visitCount_exists() {
        when(mPiwikPrefs.contains(LegacySettingsPorter.LEGACY_PREF_VISITCOUNT)).thenReturn(true);
        when(mPiwikPrefs.getInt(LegacySettingsPorter.LEGACY_PREF_VISITCOUNT, 0)).thenReturn(16);
        mPorter.port(mTracker);

        verify(mPiwikPrefs).getInt(LegacySettingsPorter.LEGACY_PREF_VISITCOUNT, 0);
        verify(mTrackerPrefs).edit();
        verify(mTrackerPrefsEditor).putLong(Tracker.PREF_KEY_TRACKER_VISITCOUNT, 16L);
        verify(mPiwikPrefsEditor).remove(LegacySettingsPorter.LEGACY_PREF_VISITCOUNT);
    }

    @Test
    public void testPort_previousVisit_empty() {
        when(mPiwikPrefs.contains(LegacySettingsPorter.LEGACY_PREF_PREV_VISIT)).thenReturn(false);
        mPorter.port(mTracker);

        verify(mTrackerPrefs, never()).edit();
        verify(mPiwikPrefs, never()).edit();
    }

    @Test
    public void testPort_previousVisit_exists() {
        when(mPiwikPrefs.contains(LegacySettingsPorter.LEGACY_PREF_PREV_VISIT)).thenReturn(true);
        when(mPiwikPrefs.getLong(LegacySettingsPorter.LEGACY_PREF_PREV_VISIT, -1)).thenReturn(1111L);
        mPorter.port(mTracker);

        verify(mPiwikPrefs).getLong(LegacySettingsPorter.LEGACY_PREF_PREV_VISIT, -1);
        verify(mTrackerPrefs).edit();
        verify(mTrackerPrefsEditor).putLong(Tracker.PREF_KEY_TRACKER_PREVIOUSVISIT, 1111L);
        verify(mPiwikPrefsEditor).remove(LegacySettingsPorter.LEGACY_PREF_PREV_VISIT);
    }

    @Test
    public void testDownloadMapping_empty() {
        final Map<String, ?> map = new HashMap<>();
        when(mPiwikPrefs.getAll()).thenAnswer(new Answer<Map<String, ?>>() {
            @Override
            public Map<String, ?> answer(InvocationOnMock invocation) throws Throwable {
                return map;
            }
        });
        mPorter.port(mTracker);

        verify(mPiwikPrefs).getAll();
        verify(mTrackerPrefs, never()).edit();
    }

    @Test
    public void testDownloadMapping_exists() {
        final Map<String, Object> map = new HashMap<>();
        String key1 = "downloaded:testkey1";
        map.put(key1, true);
        String key2 = "downloaded:testkey2";
        map.put(key2, false);
        String key3 = "testkey2";
        map.put(key3, 123465);

        when(mPiwikPrefs.getAll()).thenAnswer(new Answer<Map<String, ?>>() {
            @Override
            public Map<String, ?> answer(InvocationOnMock invocation) throws Throwable {
                return map;
            }
        });
        mPorter.port(mTracker);

        verify(mPiwikPrefs).getAll();
        verify(mPiwikPrefsEditor).remove(key1);
        verify(mPiwikPrefsEditor).remove(key2);
        verify(mPiwikPrefsEditor, never()).remove(key3);
        verify(mTrackerPrefsEditor).putBoolean(key1, true);
        verify(mTrackerPrefsEditor).putBoolean(key2, true);
        verify(mTrackerPrefsEditor, never()).putBoolean(eq(key3), anyBoolean());
    }
}
