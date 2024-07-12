/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package testhelpers

import org.robolectric.RobolectricTestRunner
import org.robolectric.TestLifecycle
import org.robolectric.util.inject.Injector

/**
 * Tries to emulate a full app environment to satisfy more in-depth tests
 */
@Suppress("unused")
open class FullEnvTestRunner : RobolectricTestRunner {
    constructor(testClass: Class<*>?) : super(testClass)

    protected constructor(testClass: Class<*>?, injector: Injector?) : super(testClass, injector)

    override fun getTestLifecycleClass(): Class<out TestLifecycle<*>?> {
        return FullEnvTestLifeCycle::class.java
    }
}
