/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package testhelpers;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.TestLifecycle;
import org.robolectric.util.inject.Injector;

import androidx.annotation.NonNull;

/**
 * Tries to emulate a full app environment to satisfy more in-depth tests
 */
@SuppressWarnings("unused")
public class FullEnvTestRunner extends RobolectricTestRunner {
    public FullEnvTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    protected FullEnvTestRunner(Class<?> testClass, Injector injector) throws InitializationError {
        super(testClass, injector);
    }

    @NonNull
    @Override
    protected Class<? extends TestLifecycle> getTestLifecycleClass() {
        return FullEnvTestLifeCycle.class;
    }
}
