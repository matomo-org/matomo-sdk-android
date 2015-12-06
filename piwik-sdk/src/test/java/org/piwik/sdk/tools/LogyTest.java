package org.piwik.sdk.tools;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.piwik.sdk.FullEnvTestRunner;
import org.robolectric.annotation.Config;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class LogyTest {
    static int level;

    @BeforeClass
    public static void patchLog() {
        level = Logy.sLoglevel;
    }

    @AfterClass
    public static void unpatchLog() {
        Logy.sLoglevel = level;
    }

    @Test
    public void testVerbose() throws Exception {
        Logy.sLoglevel = Logy.VERBOSE;
        Logy.v("PREFIX", "message");
    }

    @Test
    public void testInfo() throws Exception {
        Logy.sLoglevel = Logy.NORMAL;
        Logy.i("PREFIX", "message");
    }

    @Test
    public void testWarning() throws Exception {
        Logy.sLoglevel = Logy.NORMAL;
        Logy.w("PREFIX", "message");
        Logy.w("PREFIX", "message", new Throwable());
    }

    @Test
    public void testError() throws Exception {
        Logy.sLoglevel = Logy.NORMAL;
        Logy.e("PREFIX", "message");
        Logy.e("PREFIX", "message", new Throwable());
    }
}
