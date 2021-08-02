package org.matomo.sdk.extra;

import android.content.Intent;

import org.junit.Test;

import androidx.test.core.app.ApplicationProvider;
import testhelpers.DefaultTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;


public class InstallReferrerReceiverTest extends DefaultTestCase {
    // How to test on a live device:
    // adb shell am broadcast -a com.android.vending.INSTALL_REFERRER -n org.matomo.demo/org.matomo.sdk.extra.InstallReferrerReceiver --es "referrer" "utm_medium%3Dpartner%26utm_campaign%3Dpart
    @Test
    public void testReceiveGooglePlay() throws Exception {
        InstallReferrerReceiver receiver = new InstallReferrerReceiver();
        Intent testIntent = new Intent("com.android.vending.INSTALL_REFERRER");
        testIntent.setPackage(ApplicationProvider.getApplicationContext().getPackageName());

        String testReferrerData1 = "utm_source=test_source&utm_medium=test_medium&utm_term=test_term&utm_content=test_content&utm_campaign=test_name";
        testIntent.putExtra(InstallReferrerReceiver.ARG_KEY_GPLAY_REFERRER, testReferrerData1);
        receiver.onReceive(ApplicationProvider.getApplicationContext().getApplicationContext(), testIntent);
        Thread.sleep(250);
        String referrerDataFromPreferences = getMatomo().getPreferences().getString(InstallReferrerReceiver.PREF_KEY_INSTALL_REFERRER_EXTRAS, null);
        assertEquals(testReferrerData1, referrerDataFromPreferences);
        assertTrue(testIntent.getBooleanExtra("forwarded", false));


        String testReferrerData2 = "pk_campaign=Email-Nov2011&pk_kwd=OrderNow";
        testIntent.putExtra(InstallReferrerReceiver.ARG_KEY_GPLAY_REFERRER, testReferrerData2);

        receiver.onReceive(ApplicationProvider.getApplicationContext().getApplicationContext(), testIntent);
        Thread.sleep(250);
        referrerDataFromPreferences = getMatomo().getPreferences().getString(InstallReferrerReceiver.PREF_KEY_INSTALL_REFERRER_EXTRAS, null);
        assertEquals(testReferrerData1, referrerDataFromPreferences);


        testIntent.putExtra("forwarded", false);
        receiver.onReceive(ApplicationProvider.getApplicationContext().getApplicationContext(), testIntent);
        Thread.sleep(250);
        referrerDataFromPreferences = getMatomo().getPreferences().getString(InstallReferrerReceiver.PREF_KEY_INSTALL_REFERRER_EXTRAS, null);
        assertEquals(testReferrerData2, referrerDataFromPreferences);
    }

    @Test
    public void testGracefulFailure() throws Exception {
        InstallReferrerReceiver receiver = new InstallReferrerReceiver();
        Intent badIntent = new Intent("bad.action");
        badIntent.setPackage(ApplicationProvider.getApplicationContext().getPackageName());

        String testReferrerData1 = "utm_source=test_source&utm_medium=test_medium&utm_term=test_term&utm_content=test_content&utm_campaign=test_name";
        badIntent.putExtra(InstallReferrerReceiver.ARG_KEY_GPLAY_REFERRER, testReferrerData1);
        receiver.onReceive(ApplicationProvider.getApplicationContext().getApplicationContext(), badIntent);
        Thread.sleep(250);
        String referrerDataFromPreferences = getMatomo().getPreferences().getString(InstallReferrerReceiver.PREF_KEY_INSTALL_REFERRER_EXTRAS, null);
        assertNull(referrerDataFromPreferences);


        Intent nullIntent = new Intent();
        nullIntent.setPackage(ApplicationProvider.getApplicationContext().getPackageName());

        testReferrerData1 = "utm_source=test_source&utm_medium=test_medium&utm_term=test_term&utm_content=test_content&utm_campaign=test_name";
        nullIntent.putExtra(InstallReferrerReceiver.ARG_KEY_GPLAY_REFERRER, testReferrerData1);
        receiver.onReceive(ApplicationProvider.getApplicationContext().getApplicationContext(), nullIntent);
        Thread.sleep(250);
        referrerDataFromPreferences = getMatomo().getPreferences().getString(InstallReferrerReceiver.PREF_KEY_INSTALL_REFERRER_EXTRAS, null);
        assertNull(referrerDataFromPreferences);
    }

}
