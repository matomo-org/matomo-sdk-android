package testhelpers;

import org.junit.After;
import org.junit.Before;

import timber.log.Timber;


public class BaseTest {

    @Before
    public void setup() throws Exception {
        Timber.plant(new JUnitTree());
    }

    @After
    public void tearDown() throws Exception {
        Timber.uprootAll();
    }
}
