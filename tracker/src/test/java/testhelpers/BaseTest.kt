package testhelpers

import org.junit.After
import org.junit.Before
import timber.log.Timber
import timber.log.Timber.Forest.plant

open class BaseTest {
    @Before
    @Throws(Exception::class)
    open fun setup() {
        plant(JUnitTree())
    }

    @After
    @Throws(Exception::class)
    open fun tearDown() {
        Timber.uprootAll()
    }
}
