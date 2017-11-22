package testhelpers;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import timber.log.Timber;


@RunWith(MockitoJUnitRunner.class)
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
