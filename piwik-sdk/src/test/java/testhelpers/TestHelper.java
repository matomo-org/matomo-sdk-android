package testhelpers;

import timber.log.Timber;

public class TestHelper {

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Timber.e(e);
        }
    }
}
