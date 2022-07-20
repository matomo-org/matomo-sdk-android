package org.matomo.sdk;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import testhelpers.BaseTest;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TrackerBuilderTest extends BaseTest {
    String mTestUrl = "https://example.com/matomo.php";

    @Test
    public void testApplicationDomain() {
        Matomo matomo = mock(Matomo.class);
        Context context = mock(Context.class);
        when(matomo.getContext()).thenReturn(context);
        when(context.getPackageName()).thenReturn("some.pkg");

        TrackerBuilder trackerBuilder = new TrackerBuilder(mTestUrl, 1337, "");
        try {
            trackerBuilder.build(matomo);
        } catch (Exception ignore) {}
        assertThat(trackerBuilder.getApplicationBaseUrl(), is("https://some.pkg/"));

        trackerBuilder.setApplicationBaseUrl("rest://something");
        assertThat(trackerBuilder.getApplicationBaseUrl(), is("rest://something"));
    }

    @Test
    public void testSiteId() {
        TrackerBuilder trackerBuilder = new TrackerBuilder(mTestUrl, 1337, "");
        assertThat(trackerBuilder.getSiteId(), is(1337));
    }

    @Test
    public void testGetName() {
        TrackerBuilder trackerBuilder = new TrackerBuilder(mTestUrl, 1337, "Default Tracker");
        assertThat(trackerBuilder.getTrackerName(), is("Default Tracker"));
        trackerBuilder.setTrackerName("strawberry");
        assertThat(trackerBuilder.getTrackerName(), is("strawberry"));

    }

    @Test
    public void testEquals() {
        TrackerBuilder trackerBuilder1 = new TrackerBuilder(mTestUrl, 1337, "a");
        TrackerBuilder trackerBuilder2 = new TrackerBuilder(mTestUrl, 1337, "a");
        TrackerBuilder trackerBuilder3 = new TrackerBuilder(mTestUrl, 1336, "b");
        assertThat(trackerBuilder1, is(trackerBuilder2));
        assertThat(trackerBuilder1, is(not(trackerBuilder3)));
    }

    @Test
    public void testHashCode() {
        TrackerBuilder trackerBuilder = new TrackerBuilder(mTestUrl, 1337, "Tracker");
        int result = mTestUrl.hashCode();
        result = 31 * result + 1337;
        result = 31 * result + "Tracker".hashCode();
        assertThat(result, is(trackerBuilder.hashCode()));
    }
}
