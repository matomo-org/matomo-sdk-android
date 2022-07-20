package org.matomo.sdk.extra;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.matomo.sdk.TrackMe;
import org.matomo.sdk.Tracker;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DimensionQueueTest {
    Tracker mTracker = mock(Tracker.class);
    ArgumentCaptor<Tracker.Callback> mCaptor = ArgumentCaptor.forClass(Tracker.Callback.class);

    @Test
    public void testEmpty() {
        new DimensionQueue(mTracker);
        verify(mTracker).addTrackingCallback(mCaptor.capture());

        TrackMe pre = new TrackMe();
        TrackMe post = mCaptor.getValue().onTrack(pre);
        assertThat(post, notNullValue());
        assertThat(pre, is(post));
    }

    @Test
    public void testCallback() {
        DimensionQueue queue = new DimensionQueue(mTracker);
        verify(mTracker).addTrackingCallback(mCaptor.capture());

        queue.add(1, "test1");
        queue.add(2, "test2");
        TrackMe pre = new TrackMe();
        TrackMe post = mCaptor.getValue().onTrack(pre);
        assertThat(post, notNullValue());
        assertThat(pre, is(post));
        assertThat(CustomDimension.getDimension(post, 1), is("test1"));
        assertThat(CustomDimension.getDimension(post, 2), is("test2"));
    }

    @Test
    public void testCollision() {
        DimensionQueue queue = new DimensionQueue(mTracker);
        verify(mTracker).addTrackingCallback(mCaptor.capture());

        queue.add(1, "test1");
        TrackMe pre = new TrackMe();
        CustomDimension.setDimension(pre, 1, "don't overwrite me");
        TrackMe post = mCaptor.getValue().onTrack(pre);
        assertThat(post, notNullValue());
        assertThat(pre, is(post));
        assertThat(CustomDimension.getDimension(post, 1), is("don't overwrite me"));
    }

    @Test
    public void testOverwriting() {
        DimensionQueue queue = new DimensionQueue(mTracker);
        verify(mTracker).addTrackingCallback(mCaptor.capture());

        queue.add(1, "test1");
        queue.add(1, "test3");
        queue.add(2, "test2");
        {
            TrackMe post = mCaptor.getValue().onTrack(new TrackMe());
            assertThat(post, notNullValue());
            assertThat(CustomDimension.getDimension(post, 1), is("test1"));
            assertThat(CustomDimension.getDimension(post, 2), is("test2"));
        }
        {
            TrackMe post = mCaptor.getValue().onTrack(new TrackMe());
            assertThat(post, notNullValue());
            assertThat(CustomDimension.getDimension(post, 1), is("test3"));
            assertThat(CustomDimension.getDimension(post, 2), nullValue());
        }
    }
}
