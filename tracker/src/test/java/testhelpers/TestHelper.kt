package testhelpers

import timber.log.Timber

object TestHelper {
    @JvmStatic
    fun sleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            Timber.e(e)
        }
    }
}
