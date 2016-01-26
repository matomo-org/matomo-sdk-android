package org.piwik.sdk;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class InstallReferrerReceiverTest extends PiwikDefaultTest {

    @Test
    public void testReceiveGooglePlay() throws Exception {
        InstallReferrerReceiver receiver = new InstallReferrerReceiver();
        Intent testIntent = new Intent("com.android.vending.INSTALL_REFERRER");
        testIntent.setPackage(Robolectric.application.getPackageName());

        String testReferrerData1 = "utm_source=test_source&utm_medium=test_medium&utm_term=test_term&utm_content=test_content&utm_campaign=test_name";
        testIntent.putExtra(InstallReferrerReceiver.ARG_KEY_GPLAY_REFERRER, testReferrerData1);
        receiver.onReceive(Robolectric.application.getApplicationContext(), testIntent);
        String referrerDataFromPreferences = getPiwik().getSharedPreferences().getString(InstallReferrerReceiver.PREF_KEY_INSTALL_REFERRER_EXTRAS, null);
        assertEquals(testReferrerData1, referrerDataFromPreferences);

        String testReferrerData2 = "pk_campaign=Email-Nov2011&pk_kwd=OrderNow";
        testIntent.putExtra(InstallReferrerReceiver.ARG_KEY_GPLAY_REFERRER, testReferrerData2);
        receiver.onReceive(Robolectric.application.getApplicationContext(), testIntent);
        referrerDataFromPreferences = getPiwik().getSharedPreferences().getString(InstallReferrerReceiver.PREF_KEY_INSTALL_REFERRER_EXTRAS, null);
        assertEquals(testReferrerData2, referrerDataFromPreferences);
    }

}